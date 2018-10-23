(ns promise)

(defn create-function-call [param expr]
  "Create an sexp for calling expr with a first argument provided by a promise.
  If expr is a list (already in form suitable for a function call), insert the first argument at second position,
  otherwise turn expr into a function call expression, unless the function is an fn, which is simply returned.
  println -> (fn [param] (println param))
  (* 2)   -> (fn [param] (* param 2))

  (fn [result]) -> (fn [result])
  "
  (if (and (list? expr) (= 'fn (first expr)))               ;; expr can be used per se
    expr
    (list 'fn [param]
          (if (list? expr)
            (conj (conj (rest expr) param) (first expr))
            (list expr param)))))

(defmacro promise-> [promise & body]
  "Chain promises with an optional :catch clause. Works with any promise implementation.
  Start with a promise object and then chain as usual.
  Returns a promise object.
  (promise-> (js/Promise.resolved 1) inc inc js/console.log)
  => #object[Promise [object Promise]]
  (prints 3 on console)
  Optionally add one ore more :catch error-handler sexp to register a (.catch ...) function:
  (promise-> (js/Promise.reject \"error\") inc inc :catch js/console.error)
  => #object[Promise [object Promise]]
  (prints \"error\" on console)
  "
  (let [[body-then [_ & body-catch]] (split-with #(not= :catch %) body)
        param (gensym 'result)]

    `(-> ~promise
         ~@(map (fn [expr] (list '.then (create-function-call param expr))) body-then)
         ~@(map (fn [expr] (list `.catch (create-function-call param expr))) body-catch))))

(defmacro timeout [ms]
  `(js/Promise. (fn [resolve#] (js/setTimeout resolve# ~ms))))
