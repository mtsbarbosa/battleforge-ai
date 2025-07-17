(ns battleforge-ai.diplomat.cli
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as str]
            [battleforge-ai.controllers.deck :as deck-controller])
  (:gen-class))

(def cli-options
  [["-h" "--help" "Show help"]
   ["-v" "--verbose" "Verbose output"]
   ["-c" "--config CONFIG" "Configuration file path"
    :default "config.edn"]])

(def battle-options
  [["-1" "--deck1 DECK1" "Path to first deck file" :required true]
   ["-2" "--deck2 DECK2" "Path to second deck file" :required true]
   ["-b" "--battles BATTLES" "Number of battles to simulate"
    :default 100
    :parse-fn #(Integer/parseInt %)]
   ["-t" "--timeout TIMEOUT" "Timeout per game in minutes"
    :default 30
    :parse-fn #(Integer/parseInt %)]
   ["-o" "--output OUTPUT" "Output file path"]])

(def simulate-options
  [["-d" "--decks-dir DIR" "Directory containing deck files" :required true]
   ["-f" "--format FORMAT" "Simulation format (tournament, round-robin, custom)"
    :default "round-robin"]
   ["-b" "--battles-per-match BATTLES" "Battles per matchup"
    :default 100
    :parse-fn #(Integer/parseInt %)]
   ["-p" "--parallel WORKERS" "Number of parallel workers"
    :default 4
    :parse-fn #(Integer/parseInt %)]
   ["-o" "--output OUTPUT" "Output file path"]])

(def stats-options
  [["-i" "--input INPUT" "Input results file" :required true]
   [nil "--output-format FORMAT" "Output format (json, csv, html)"
    :default "json"]
   ["-m" "--metrics METRICS" "Specific metrics to analyze (comma-separated)"]
   ["-o" "--output OUTPUT" "Output file path"]])

(def fetch-deck-options
  [["-s" "--source SOURCE" "Source to fetch from (keyforge, dok, search)"
    :default "keyforge"]
   ["-o" "--output-dir DIR" "Output directory for deck file"
    :default "./decks"]
   ["-n" "--name NAME" "Deck name (for search source)"]
   [nil "--overwrite" "Overwrite existing deck file"]])

(defn usage [options-summary]
  (->> ["BattleForge AI - Keyforge Deck Battle Simulator"
        ""
        "Usage: battleforge-ai [global-options] command [command-options]"
        ""
        "Global options:"
        options-summary
        ""
        "Commands:"
        "  battle      Simulate battles between two specific decks"
        "  simulate    Run mass simulations with multiple decks"
        "  stats       Generate statistics from simulation results"
        "  fetch-deck  Fetch a deck from online sources and save as JSON"
        ""
        "Use 'battleforge-ai command --help' for command-specific options."]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with an error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options :in-order true)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      
      (empty? arguments) ; no command provided
      {:exit-message (usage summary)}
      
      :else ; valid command
      {:command (first arguments)
       :command-args (rest arguments)
       :options options})))

(defn execute-battle [args options]
  (println "🗡️  Battle simulation mode")
  (println "⚠️  This feature is not yet implemented")
  (println "Args:" args)
  (println "Options:" options)
  :executed)

(defn execute-simulate [args options]
  (println "⚔️  Mass simulation mode")
  (println "⚠️  This feature is not yet implemented")
  (println "Args:" args)
  (println "Options:" options)
  :executed)

(defn execute-stats [args options]
  (println "📊 Statistics analysis mode")
  (println "⚠️  This feature is not yet implemented")
  (println "Args:" args)
  (println "Options:" options)
  :executed)

(defn execute-fetch-deck [args options]
  (println "🃏 Fetch deck mode")
  (let [deck-id (first args)
        source (:source options)
        output-dir (:output-dir options)
        deck-name (:name options)
        overwrite? (:overwrite options)]
    
    (cond
      (and (not deck-id) (not deck-name))
      (println "❌ Error: Must provide either deck ID or deck name")
      
      :else
      (try
        (println "📡 Fetching deck from" source "...")
        
        ;; Call controller instead of handling business logic directly
        (deck-controller/fetch-deck! 
          {:deck-id deck-id
           :source source
           :output-dir output-dir
           :deck-name deck-name
           :overwrite? overwrite?
           :verbose? (:verbose options)})
        
        (catch Exception e
          (println "❌ Error fetching deck:" (.getMessage e))
          (when (:verbose options)
            (println "Stack trace:")
            (.printStackTrace e)))))))

(defn execute-command [command args global-options]
  (case command
    "battle" (let [{:keys [options arguments errors summary]} 
                   (parse-opts args (concat cli-options battle-options))]
               (if errors
                 (do (println (error-msg errors))
                     (System/exit 1))
                 (execute-battle arguments (merge global-options options))))
    
    "simulate" (let [{:keys [options arguments errors summary]} 
                     (parse-opts args (concat cli-options simulate-options))]
                 (if errors
                   (do (println (error-msg errors))
                       (System/exit 1))
                   (execute-simulate arguments (merge global-options options))))
    
    "stats" (let [{:keys [options arguments errors summary]} 
                  (parse-opts args (concat cli-options stats-options))]
              (if errors
                (do (println (error-msg errors))
                    (System/exit 1))
                (execute-stats arguments (merge global-options options))))
    
    "fetch-deck" (let [{:keys [options arguments errors summary]} 
                       (parse-opts args (concat cli-options fetch-deck-options))]
                   (if errors
                     (do (println (error-msg errors))
                         (System/exit 1))
                     (execute-fetch-deck arguments (merge global-options options))))
    
    (do (println (str "Unknown command: " command))
        (System/exit 1))))

(defn -main
  "Main entry point for BattleForge AI CLI"
  [& args]
  (let [{:keys [command command-args options exit-message ok?]} (validate-args args)]
    (if exit-message
      (do (println exit-message)
          (System/exit (if ok? 0 1)))
      (do
        (when (:verbose options)
          (println "🚀 Starting BattleForge AI...")
          (println "Command:" command)
          (println "Global options:" options))
        (execute-command command command-args options)))))