def a = ["pig":"oink", "cow":"moo", "horse":"neigh"]

a.each{ println "The ${it.key} says ${it.value}" }
