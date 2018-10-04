(ns chlorine.core)

(defmacro defcmd [name & cmds]
  `(do
     (defn ~name [] ~@cmds)
     (command-for ~(str name) ~name)))
