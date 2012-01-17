from scipy import stats
import operator
import networkx
import utility
import numpy
import conf
import sim

def ego_out_edges(G):
    #compute the number of all the nodes' ego out edges
    ego_out_dic = {}
    for n in G.nodes():
        ego = networkx.generators.ego.ego_graph(G,n);
        nodes_in_ego = set(ego.nodes())
        cnt = 0
        for e in ego.edges():
            if(e[0] in nodes_in_ego or e[1] not in nodes_in_ego):
                continue
            cnt+=1
        ego_out_dic[n] = cnt
    return ego_out_dic

def ego_neighbors(G):
    ego_neib_dic = {}
    cnt = 0
    for n in G.nodes():
        ego = networkx.generators.ego.ego_graph(G,n);
        nodes_in_ego = set(ego.nodes())
        t = set()
        for w in nodes_in_ego:
            t|=set(G.neighbors(w))
        ego_neib_dic[n] = len(t)
        cnt+=1
    return ego_neib_dic

def average_clustering_of_neighbors(G):
    clustering = networkx.algorithms.cluster.clustering(G, weighted=conf.weighted)
    res_dic = {}
    for n in G.nodes():
        cnt = 0
        sum = 0.0
        for o in G.neighbors(n):
            sum += clustering[o];
            cnt+=1
        if(cnt==0):
            res_dic[n] = 0
        else:
            res_dic[n] = sum*1.0/cnt
    return res_dic

def ego_edges(G):
    edges = {}
    for n in G.nodes():
        ego = networkx.generators.ego.ego_graph(G,n);
        edges[n] = len(ego.edges())
    return edges

def get_feature(nodes, degrees, clustering, n_ego_edges, n_ego_out_edges, n_ego_neighbors, avg_in_degree_n, avg_out_degree_n, avg_clustering_n):
    features = [ [], [], [], [], [], [], [], []]
    for n in nodes:
        features[0].append(degrees[n]);
        features[1].append(clustering[n]);
        features[2].append(avg_in_degree_n[n])
        features[3].append(avg_out_degree_n[n])
        features[4].append(avg_clustering_n[n]);
        features[5].append(n_ego_edges[n] - n_ego_out_edges[n]);
        features[6].append(n_ego_out_edges[n]);
        features[7].append(n_ego_neighbors[n]);
    #now aggregate the features
    f = []
    for i in range(8):
        #print features[i]
        median = numpy.median(features[i]);
        mean = numpy.mean(features[i]);
        stdev = numpy.std(features[i]);
        skewness = stats.skew(features[i]);
        kurtosis = stats.kurtosis(features[i]);
        f += [median, mean, stdev, skewness, kurtosis];
    return f

def compute_vector_score(v_1, v_2):
    v_1 = numpy.array(v_1)
    v_2 = numpy.array(v_2)
    s = numpy.sqrt(numpy.sum( ((v_1-v_2)**2)))
    return s

def compute_net_feature(G, community_nodes):
    print "computing netsimile features"
    features = {}
    undirected_G = G.to_undirected()
    if(conf.undirected):
        avg_in_degree_n = networkx.average_neighbor_degree(G)
        avg_out_degree_n = avg_in_degree_n
    else:
        avg_in_degree_n = networkx.algorithms.neighbor_degree.average_neighbor_in_degree(G,weighted=conf.weighted)
        avg_out_degree_n =networkx.algorithms.neighbor_degree.average_neighbor_out_degree(G,weighted=conf.weighted)
    avg_clustering_n = average_clustering_of_neighbors(undirected_G )
    degrees = G.degree(weighted=conf.weighted)
    clustering = networkx.algorithms.cluster.clustering(undirected_G,weighted=conf.weighted);
    n_ego_edges = ego_edges(undirected_G)
    n_ego_out_edges = ego_out_edges(G);
    n_ego_neighbors = ego_neighbors(G);
    cnt = 0
    for k in community_nodes.keys():
        features[k] = get_feature(community_nodes[k], degrees, clustering, n_ego_edges, n_ego_out_edges, n_ego_neighbors, avg_in_degree_n, avg_out_degree_n, avg_clustering_n) 
        #print cnt
        cnt+=1
    print "computing done"
    return features

def net_simile( P_ref, P_tar):
    print "debug"
    #do net similarity check for each partition in G_ref and G_tar
    ref_community_nodes = {}
    tar_community_nodes = {}
    for n in P_ref:
        ref_community_nodes.setdefault(P_ref[n],[]).append(n)
    for n in P_tar:
        tar_community_nodes.setdefault(P_tar[n],[]).append(n)
    G_ref = utility.open_graph(conf.ref_graph_file)
    G_tar = utility.open_graph(conf.target_graph_file)
    ref_partition_features = compute_net_feature(G_ref, ref_community_nodes)
    tar_partition_features = compute_net_feature(G_tar, tar_community_nodes)
    
    #number_of_ele = min(len(tar_community_nodes.keys()), len(ref_community_nodes.keys()))
    #number_of_ele = min(1000, number_of_ele)
    #sum_score = 0
    sim_table = {} #store for each community in target the list of similar communities in ref

    #ref_feature_table = utility.read_feature_table(conf.ref_feature_file)
    #tar_feature_table = utility.read_feature_table(conf.target_feature_file)
    #guess_range = (int)(conf.percentage*ref_size)
    cnt = 0; 
    ref_partition_feature_matrix = []
    ref_keys = ref_partition_features.keys()
    for key in ref_keys:
        ref_partition_feature_matrix.append(ref_partition_features[key])
    ref_partition_feature_matrix = numpy.array(ref_partition_feature_matrix)
    
    for k in set(tar_community_nodes.keys()):
        f_tar = tar_partition_features[k]
        sim_list = []
        print cnt
        cnt+=1
        sim_score = sim.compute_distance(m=ref_partition_feature_matrix, v=numpy.array(f_tar))
        sim_list = sorted(zip( ref_keys,sim_score), key=operator.itemgetter(1,0))
        #print sim_list
        """sim_list = []
        for l in set(ref_community_nodes.keys()):
            f_ref = ref_partition_features[l]
            s = compute_vector_score(f_ref,f_tar)
            sim_list.append((l, s))
        sim_list = sorted(sim_list, key=operator.itemgetter(1,0))
        """
        sim_table[k] = sim_list
        """nodes_tar = tar_community_nodes[k]
        inter = 0
        len_sum = 0
        for i in range(number_of_ele):
            nodes_ref = ref_community_nodes[sim_list[i][0]]
            len_sum+=len(nodes_ref)
            if(len_sum>=guess_range):
                inter+= guess_range*1.0*len(set(nodes_ref)&set(nodes_tar))/len(nodes_ref)
                break;
            inter += len(set(nodes_ref)&set(nodes_tar))
        sum_score += inter"""
    return sim_table,  ref_community_nodes, tar_community_nodes
