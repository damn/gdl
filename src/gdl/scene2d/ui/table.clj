(ns gdl.scene2d.ui.table)

(defprotocol Table
  (cells [_])
  (add-rows [_ rows]
            "Cell opts & map-cell..."))
