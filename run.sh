java -XX:-MaxFDLimit -Ddatabase.url="postgres://technobabble:testdb@localhost/technobabble_dev" -Dport=3333 -cp target/technobabble.jar clojure.main -m technobabble.core

