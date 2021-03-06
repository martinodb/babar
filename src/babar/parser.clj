(ns babar.parser
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]
            [babar.commands :refer :all]
            [babar.speech-acts :refer :all]))

(declare parse)

(def parser
  (insta/parser
   "program =   (expr / vector) / (expr <('.'|'?')> space*)+
    expr = item | command | functioncall | readprogram
    readprogram = <'read'> space string
    command = commandkey space vector |
               <'('> (space)* commandkey space vector (space)* <')'>
    commandkey = operation | special
    functioncall =  <'('> item  <')'> /
                    <'('> item space vector <')'> /
                    item <':'> space vector /
                    item <':'>
    map = <'{'> ((space)* item (space)*)+ <'}'>
    <vector>  = svector | bvector
    svector = ((space)* item (space)*)+
    bvector =  <#'\\['> ((space)* item (space)*)+ <#'\\]'> |
               <#'\\[\\]'>
    <space> = <#'[\\s\\t\\n\\,]+'>
    <item> = command / speech-act / deref / functioncall / string / number / boolean /
             keyword / bvector / map / identifier
    speech-act = commitment | belief | query | request | convince | assertion | speak-config | ask-config
    assertion = (<'assert'> | <'defn'>) space #'[a-z][0-9a-zA-Z\\-\\_]*' space bvector space item /
                (<'assert'> | <'def'>) space #'[a-z][0-9a-zA-Z\\-\\_]*' space item
    query = 'query' space querytype space (commitment | belief) /
            'query' space querytype space identifier /
            'query' space querytype /
            'ask-query' space  #'[a-z][0-9a-zA-Z\\-\\_]*'
    querytype = 'request-value' | 'request-details' | 'request-completed' |
                'request-created' | 'request-errors' | 'request-fn' |
                'request-when' | 'request-is-done' | 'request-until' |
                'request-ongoing' | 'request-cancelled' | 'belief-str' | 'belief-fn' |
                'requests-all' | 'beliefs-all' | 'value'
    request =   'request' space <'*'>  #'[a-z][0-9a-zA-Z\\-\\_]*' space
                   ('when'| 'until' | 'ongoing') space belief space expr  /
                 'request' space <'*'>  #'[a-z][0-9a-zA-Z\\-\\_]*' space
                   'when' space belief space 'until' space belief space expr  /
                 'request' space <'*'>  #'[a-z][0-9a-zA-Z\\-\\_]*' space
                   'when' space belief space 'ongoing' space expr  /
                'request' space <'*'>  #'[a-z][0-9a-zA-Z\\-\\_]*' space
                  'ongoing' space expr /
                'request' space <'*'>  #'[a-z][0-9a-zA-Z\\-\\_]*' space expr /
                'request-cancel' space  <'*'> #'[a-z][0-9a-zA-Z\\-\\_]*'
    convince = 'convince' space <'#'> #'[a-z][0-9a-zA-Z\\-\\_]*'
               space string space expr
    speak-config = <'speak-config'> space boolean /
                    <'speak-config'> space boolean space string
    ask-config = <'ask-config'> space boolean
    commitment = <'*'> #'[a-z][0-9a-zA-Z\\-\\_]*'
    belief = <'#'> #'[a-z][0-9a-zA-Z\\-\\_]*'
    <operation> =  '+' | '-' | '*' | '/'
    deref = <'@'> identifier
    identifier =  #'[a-z][0-9a-zA-Z\\-\\_]*'
    <special> = 'if' | '=' | '<' | '>' | 'and' | 'or'
                | 'import' | 'fn' | 'println' | 'get' | 'do' | 'sleep' | 'first' |
                'atom' | 'swap!' | 'reset!'
    string =  <'\\\"'> #'([^\"\\\\]|\\\\.)*' <'\\\"'>
    keyword = <#'[:]'> #'[\\w|-]+'
    boolean = #'true' | #'false'
    number = integer | decimal
    <decimal> = #'-?[0-9]+\\.[0-9]+'
    <integer> = #'-?[0-9]+'"))


(defn babar-eval [expr]
  (eval expr))

(defn eval-program [expr-list]
  (let [result (babar-eval (first expr-list))]
    (if (empty? (rest expr-list)) result (recur (rest expr-list)))))

(defn read-program [filename]
  `(parse (slurp ~filename)))

(def transform-options
  {:number read-string
   :string str
   :keyword keyword
   :boolean read-string
   :svector (comp vec list)
   :bvector (comp vec list)
   :map hash-map
   :deref babar-deref
   :assertion babar-assert
   :commitment commitment
   :belief belief
   :request request
   :convince convince
   :speech-act identity
   :identifier babar-indentifier
   :commandkey identity
   :command babar-command
   :functioncall babar-functioncall
   :expr identity
   :querytype identity
   :query query
   :readprogram read-program
   :speak-config speak-config
   :ask-config ask-config
   :program (comp eval-program list)})

(defn parse [input]
  (->> (parser input) (insta/transform transform-options)))

(defn init []
  (init-commitments))

