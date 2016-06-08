{:fq-path-to-relative (fn [fq-path]
                        (let [root (System/getProperty "user.dir")
                              size (count root)]
                          (.substring fq-path size)))

 :symbols-in-project (fn []
                       (let [rt (clojure.lang.RT/baseLoader)]
                         (for [namespace (->> (all-ns))
                               :let [ns-name (.name namespace)]
                               symbol-map (ns-publics ns-name)
                               :let [fq-symbol (.val symbol-map)
                                     file-path (-> fq-symbol meta :file)
                                     file-str (when file-path
                                                (some-> rt
                                                        (.getResource file-path)
                                                        (.getFile)))]
                               :when (when file-str (not (re-find #"\.jar!" file-str)))]
                           (str ns-name "/" (.key symbol-map)))))

 :find-dependencies (fn [symbols to-find]
                      (let [search (fn search [sexp]
                                     (if (coll? sexp)
                                       (some search sexp)
                                       (= sexp to-find)))
                            have-sym? (fn [s]
                                        (let [sym (symbol s)
                                              source (try (clojure.repl/source-fn sym) (catch Exception _))
                                              quoted (str "`" source)]
                                          (in-ns (-> s (clojure.string/split #"/") first symbol))
                                          (search (try (load-string quoted) (catch Exception e)))))]
                        (filter have-sym? symbols)))}


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
