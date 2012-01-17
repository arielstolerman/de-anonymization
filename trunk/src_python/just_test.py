import utility
import conf
import math

def needs_a_graph():
    G = utility.open_graph(conf.ref_graph_file)
    print "in needs"
    print G.number_of_nodes()
    for j in range(1,3001000):
        t = math.log(j)
    print "out"

for i in range(20):
    #G = utility.open_graph(conf.ref_graph_file)
    for j in range(1,3000000):
        t = math.log(j)
    needs_a_graph()
