(def libgdx-version "1.13.1")

(defproject gdl "-SNAPSHOT"
  :repositories [["jitpack" "https://jitpack.io"]]
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [com.badlogicgames.gdx/gdx                ~libgdx-version]
                 [com.badlogicgames.gdx/gdx-platform       ~libgdx-version :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-backend-lwjgl3 ~libgdx-version]
                 [space.earlygrey/shapedrawer "2.5.0"]
                 [com.badlogicgames.gdx/gdx-freetype          "1.13.0"]
                 [com.badlogicgames.gdx/gdx-freetype-platform "1.13.0" :classifier "natives-desktop"]
                 [com.kotcrab.vis/vis-ui "1.5.2"]]
  :java-source-paths ["src-java"]
  :plugins [[lein-hiera "2.0.0"]
            [lein-codox "0.10.8"]]
  :codox {:source-uri "https://github.com/damn/gdl/blob/main/{filepath}#L{line}"
          :metadata {:doc/format :markdown}}
  :global-vars {*warn-on-reflection* true
                ;*unchecked-math* :warn-on-boxed
                ;*assert* false
                ;*print-level* 3
                })
