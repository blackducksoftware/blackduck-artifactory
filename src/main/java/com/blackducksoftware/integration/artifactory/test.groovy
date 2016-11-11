def a = ["pig":"oink", "cow":"moo", "horse":"neigh", "Ari":"I'm stupid"]

a.each{ println "The ${it.key} says ${it.value}" }
