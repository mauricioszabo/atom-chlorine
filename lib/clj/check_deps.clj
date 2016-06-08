(ns check-deps)

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
                      (in-ns (-> s (clojure.string/split #"/") first symbol))
                      (search (try (load-string quoted) (catch Exception e)))))]
    (filter have-sym? symbols)))


; ((fn [symbols to-find]
;     (let [search (fn search [sexp]
;                    (if (coll? sexp)
;                      (some search sexp)
;                      (= sexp to-find)))
;           have-sym? (fn [s]
;                       (let [sym (symbol s)
;                             source (try (clojure.repl/source-fn sym) (catch Exception _))
;                             quoted (str "`" source)]
;                         (in-ns (-> s (clojure.string/split #"/") first symbol))
;                         (search (try (load-string quoted) (catch Exception e)))))]
;       (filter have-sym? symbols))) all-ss 'accounts.double-entry.models.adjustment/schema)
