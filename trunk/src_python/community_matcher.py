from time import clock
import partition
import netsimile
import cluster_sim
import operator
import utility
import numpy
import conf
import sys
import sim

def community_based_ReId(P_tar, P_ref, sim_table, ref_feature_table, tar_feature_table, ref_community_nodes, tar_community_nodes):
    """
    Basically, P_tar is a dic. for a key k, P_tar[k] is the community k belongs to.(the same with P_ref).
    sim_table is a dic in which keeps the list of alike communities. for community c, sim_table[c] is a list of alike communities for community c in reference graph.
    ref_feature_table and tar_feature_table are what they are called.
    """
    #now do the node level match
    ref_size = len(ref_feature_table) 
    print "ref size %d"%ref_size
    guess_range = int(conf.percentage*ref_size)
    top_nodes = utility.find_top_degree_nodes()
    print "length of top_nodes %d"%len(top_nodes)
    
    [cnt, cannot_predict_cnt, good_guess, sum_score] = [0]*4;
    ref_mapper = {}
    ind = 0
    whole_matrix = []
    for r in (ref_feature_table.keys() ):
        ref_mapper[r] = ind
        ind+=1
        whole_matrix.append(ref_feature_table[r])
    whole_matrix = numpy.array(whole_matrix)    
    for target_node in top_nodes:
        print "working on "+str(cnt) +'node. Trying to identify -> '+target_node
        cnt+=1
        #find the community a target node target_node belongs to
        com = P_tar[target_node]
        sim_list = sim_table[com]
        print "length of com list %d"%len(sim_list)
        try:
            tar_feature = tar_feature_table[target_node]
        except:
            print "can't predict for "+target_node+", continue"
            cannot_predict_cnt+=1
            continue
        
        length_sum = 0
        ref_node_list = []
        ref_node_index_list = []
        for i in range(len(sim_list)):
            ref_nodes = ref_community_nodes[sim_list[i][0]]
            #ref_node_list += ref_nodes
            t_cnt = 0
            
            for n in ref_nodes:
                #if(n in ref_mapper):
                try:
                    ref_node_index_list.append(ref_mapper[n])
                    t_cnt +=1
                    ref_node_list.append(n)
                except:
                    continue
            #ref_node_index_list += [ref_mapper[n] for n in ref_nodes if n in ref_mapper]
            length_sum+=t_cnt;

            if(length_sum>conf.nodes_pool_ratio*ref_size):
                break
        matrix = whole_matrix[ref_node_index_list]
        w = sim.compute_distance(m=matrix, v = tar_feature, method='euclidean')
        similarity_score = (zip(ref_node_list, w))
        
        sort_list = sorted(similarity_score, key=operator.itemgetter(1,0))
        for i in range( min(guess_range,length_sum)):
            if(sort_list[i][0]==target_node):
                if(i<=guess_range):
                    good_guess+=1
                    sum_score+=good_guess
                print "Now good guess is %d, score is %d %s"%(good_guess, i, target_node) 
                break
    utility.report_printer(sum_score, good_guess)

def community_matcher():
    G_ref = utility.open_graph(conf.ref_graph_file);
    G_tar = utility.open_graph(conf.target_graph_file);
    ref_feature_table = utility.read_feature_table(conf.ref_feature_file)
    tar_feature_table = utility.read_feature_table(conf.target_feature_file)
    
    #Partition here
    print "Partitioning graphs..."
    P_tar, P_ref = partition.partition(conf.target_graph_file,G_tar,tar_feature_table,
                                       conf.ref_graph_file,G_ref,ref_feature_table)
    print "done partitioning graphs!"
    # original:
    #print "Partitioning reference graph"
    #P_ref = partition.partition(conf.ref_graph_file,G_ref,ref_feature_table)
    #print "done"
    #print "Partitioning target graph"
    #P_tar = partition.partition(conf.target_graph_file,G_tar,tar_feature_table)
    #print "done"
	
    """
    so the most important part netsimile return is: 
    sim_table
    this is the varible contain the similarity value for each subgraph in Reference. 
    e.g. sim_table[k] return a list, which contains the similarities for k in target graph with all the subgraphs in Reference graph. 
    it would be look like
    sim_talbe[3] = [(3,0.99), (2,0.22), (1,0.13)]
    this means subgraph 3 in target graph has the similar subgraphs in ref-graph with 0.99, 0.22, 0.13 of similarities. 
    """
    start = clock()
    if(conf.cluster_sim_measure == 'netsimile'):
        sim_table, ref_community_nodes, tar_community_nodes = netsimile.net_simile(P_ref, P_tar)
        #original 
        #sim_table, ref_feature_table, tar_feature_table, ref_community_nodes, tar_community_nodes = netsimile.net_simile(G_ref, G_tar, P_ref, P_tar);
    elif(conf.cluster_sim_measure == 'cluster_rank'):
        sim_table, ref_community_nodes, tar_community_nodes = cluster_sim.cluster_rank_sim(P_ref, P_tar)
    elif(conf.cluster_sim_measure == 'cluster_id'):
        sim_table, ref_community_nodes, tar_community_nodes = cluster_sim.cluster_id_sim(P_ref, P_tar)

    community_based_ReId(P_tar, P_ref, sim_table, ref_feature_table, tar_feature_table, ref_community_nodes, tar_community_nodes)
    
    
    
    end = clock()
    print "run time"
    print end-start

if __name__=='__main__':
    """change here according to what you want to experiment"""
    args = sys.argv[1:]
    
    conf.ref_graph_file = args[0]
    conf.ref_feature_file = args[1]
    conf.target_graph_file = args[2]
    conf.target_feature_file = args[3]

    #partition methods, metis_args, number of clusters you could change here, and change the command in "run_community.py"
    conf.nodes_pool_ratio = (float)(args[4])
    conf.partition_method = args[5]
    conf.num_of_clusters = (int)(args[6])
    conf.cluster_sim_measure = args[7]
    
    utility.statistics()
    community_matcher()
