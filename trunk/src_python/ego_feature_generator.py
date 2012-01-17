import networkx as nx
import utility
import getopt
import conf
import sys

def usage():
    msg = """
    For the two graphs,
    conf.ref_graph_file,and conf.target_graph_file
    generate the 5 features for them. and save the 
    feature files as prefix-reference-ego-features.txt
    and prefix-target-ego-features.txt.
    
    features are (for node N)
    1. number of neiborhoods in ego(N)
    2. clustering coefficient of node N
    3. average neighborhoods degree for ego(N)
    4. average clustering coefficient for ego(N)
    5. number of egdes for ego(N)
    6. pagerank*number of nodes for node graph (this is for add weight for pr value cuz it's usually toooooo small for euclidean distance)


    Usage:
    -h for help
    -p prefix for feature files. 
       e.g. -p sms_wire will generate two files
       sms_wire-reference-ego-features.txt and
       sms_wire-target-ego-features.txt.
    NOTICE:
    If the feature files have already been generated(in /data), 
    then it will not    generate new feature files. 
    """
    print msg

def feature_generator(G, ego, node):
    """generate the features for each node"""
    n_neighbors = nx.degree(G,node);  #number of degree
    clustering_coefficient = nx.clustering(G,node);
    avg_neighbor_degree = nx.average_neighbor_degree(G, node).values()[0];
    avg_clustering_coefficient = nx.algorithms.cluster.average_clustering(ego);
    n_edges = ego.number_of_edges();
    t = [n_neighbors, clustering_coefficient, avg_neighbor_degree, avg_clustering_coefficient, n_edges];
    return t

def ego_table(G):
    ego_t = {}
    #hub, authority = nx.algorithms.link_analysis.hits_alg.hits_scipy(G)
    pr_value = nx.algorithms.link_analysis.pagerank_alg.pagerank_scipy(G)
    graph_size = G.number_of_nodes()
    for n in G.nodes():
        fs = feature_generator(G, nx.ego_graph(G,n), n)
        ego_t[n] = fs
        ego_t[n].append(graph_size*pr_value[n])
        #ego_t[n].append(conf.hub_weight*hub[n]);
        #ego_t[n].append(conf.authority_weight*authority[n]);
    return ego_t


def ego_feature(prefix):
    """Usage:
    for a given graph, we do the following
    1. extract ego-net for each node, say node i
    2. get the 7 features
    """    
    should_exit = False
    try:
        f = open('./data/'+prefix+"-reference-ego-features.txt",'r');
        print "Reference feature file already exists!"
        should_exit = True
    except:
        pass
    try:
        f = open('./data/'+prefix+"-target-ego-features.txt",'r');
        print "Target feature file already exists!"
    except:
        pass
    if(should_exit):
        sys.exit(2)

    ego_feature_dic = ego_table(utility.open_graph(conf.ref_graph_file))
    print "trying" 
        
    f = file('./data/'+prefix+"-reference-ego-features.txt",'w');
    for k in ego_feature_dic.keys():
        f.write(k)
        for value in ego_feature_dic[k]:
            f.write(',%.20f'%value);
        f.write('\n')
    f.close()    

    ego_feature_dic = ego_table(utility.open_graph(conf.target_graph_file))
    f = file('./data/'+prefix+"-target-ego-features.txt",'w');
    for k in ego_feature_dic.keys():
        f.write(k)
        for value in ego_feature_dic[k]:
            f.write(',%.20f'%value);
        f.write('\n')
    f.close()   

def main(argv):
    try:
        opts, args = getopt.getopt(argv, "p:h")
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    print opts
    if len(opts)==0 or ("-p") not in list(opts[0]):
        usage()
        sys.exit(2)
    for opt, arg in opts:
        if opt in ("-h"):
            usage()
        elif opt in ("-p"):
            prefix = arg;
            ego_feature(prefix)


if __name__ == "__main__":
    main(sys.argv[1:])
