(ns --check-deps--
  (:require [clojure.string :refer [split join]]))

(defn vars-in-form [form vars]
  (cond
    (coll? form) (reduce #(vars-in-form %2 %1) vars form)
    (symbol? form) (conj vars form)
    :else vars))

(defn vars-for-file [file-name]
  (let [code (load-string (str "'(" (slurp  file-name) "\n)"))
        filtered (filter #(not= 'ns (first %)) code)]
    (reduce (fn [vars list-of-forms]
              (vars-in-form (load-string (str "`" list-of-forms)) vars))
            #{} filtered)))

(defn unused-namespaces [file]
  (let [nss-in-file (->> file
                         vars-for-file
                         (map str)
                         (filter #(re-find #"/" %))
                         (map #(first (clojure.string/split % #"/")))
                         set)
        parsed-ns (refactor-nrepl.ns.ns-parser/parse-ns file)
        requires (map #(-> % :ns str) (concat (get-in parsed-ns [:clj :require]) (get-in parsed-ns [:cljs :require])))]
    (remove #(contains? nss-in-file %) requires)))

(defn fq-path-to-relative [fq-path]
  (let [root (System/getProperty "user.dir")
        size (count root)]
    (if (.startsWith fq-path root)
      (.substring fq-path (inc size))
      fq-path)))

(defn symbols-in-project []
  (let [rt (clojure.lang.RT/baseLoader)]
    (for [namespace (->> (all-ns))
          :let [ns-name (.name namespace)]
          symbol-map (ns-publics ns-name)
          :let [fq-symbol (.val symbol-map)
                meta-symbol (meta fq-symbol)
                file-path (:file meta-symbol)
                file-str (when file-path
                           (some-> rt
                                   (.getResource file-path)
                                   (.getFile)))]
          :when (when file-str (not (re-find #"\.jar!" file-str)))]
      (assoc meta-symbol :fqname (str ns-name "/" (.key symbol-map))
                         :fqpath file-str))))

(defn find-dependencies [symbols to-find]
  (let [search (fn search [sexp]
                 (if (coll? sexp)
                   (some search sexp)
                   (= sexp to-find)))
        have-sym? (fn [s]
                    (let [sym (-> s :fqname symbol)
                          source (try (clojure.repl/source-fn sym) (catch Exception _))
                          quoted (str "`" source)]
                      (in-ns (-> s (split #"/") first symbol))
                      (search (try (load-string quoted) (catch Exception e)))))]
    (filter have-sym? symbols)))

(defn decompress-all [temp-dir line [jar-path partial-jar-path within-file-path]]
  (let [decompressed-path (str temp-dir "/" partial-jar-path)
        decompressed-file-path (str decompressed-path "/" within-file-path)
        decompressed-path-dir (clojure.java.io/file decompressed-path)]
    (when-not (.exists decompressed-path-dir)
      (println "decompressing" jar-path "to" decompressed-path)
      (.mkdirs decompressed-path-dir)
      (clojure.java.shell/sh "unzip" jar-path "-d" decompressed-path))
    [decompressed-file-path line]))

(defn goto-var [var-sym temp-dir]
  (require 'clojure.repl)
  (require 'clojure.java.shell)
  (require 'clojure.java.io)
  (let [the-var (or (some->> (or (get (ns-aliases *ns*) var-sym) (find-ns var-sym))
                             clojure.repl/dir-fn
                             first
                             name
                             (str (name var-sym) "/")
                             symbol)
                    var-sym)
        {:keys [file line]} (meta (eval `(var ~the-var)))
        file-path (.getPath (.getResource (clojure.lang.RT/baseLoader) file))]
    (if-let [[_ & jar-data] (re-find #"file:(.+/\.m2/repository/(.+\.jar))!/(.+)" file-path)]
      (decompress-all temp-dir line jar-data)
      [(clojure.string/replace file-path #"/project/" "") line])))

(defn symbols-from-ns [ns-ref]
  (for [sym-name (map first (ns-interns ns-ref))
        :let [ns-name (.name ns-ref)
              fq-str (str "#'" ns-name "/" sym-name)
              fqname (load-string fq-str)]]
    (merge {:ns ns-name :symbol sym-name} (select-keys (meta fqname) [:line :column]))))

(defn symbols-from-ns-in-json [ns-ref]
  (let [syms (symbols-from-ns ns-ref)
        jsons (map #(str "{\"line\":" (:line %)
                         ",\"column\":" (:column %)
                         ",\"symbol\":\"" (:symbol %) "\""
                         "}")
                   syms)]
    (str "[" (join "," jsons) "]")))

(defn resolve-missing [name]
  (let [edn-str (or (refactor-nrepl.ns.resolve-missing/resolve-missing {:symbol name}) "[]")
        namespace-names (->> edn-str read-string (map :name) (apply sorted-set))
        aliases (for [aliases (mapcat second (refactor-nrepl.ns.libspecs/namespace-aliases))
                      :let [[alias namespaces] aliases]
                      ns-name namespaces
                      :when (contains? namespace-names ns-name)]
                  [ns-name alias])
        alias-map (group-by first aliases)]
    (mapcat #(or (get alias-map %) [[% nil]]) namespace-names)))
