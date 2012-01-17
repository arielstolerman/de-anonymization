import operator

def cluster_rank_sim(P_ref, P_tar):
    # calculate similarity between clusters by -|tar_cluster.order_statistic - ref_cluster.order_statistic|
    # so as two clusters are closer by their order statistic, the larger the sim score is
    
    ref_community_nodes = {}
    tar_community_nodes = {}
    for n in P_ref:
        ref_community_nodes.setdefault(P_ref[n],[]).append(n)
    for n in P_tar:
        tar_community_nodes.setdefault(P_tar[n],[]).append(n)
    ref_node_count = {}
    ref_os = {}
    tar_node_count = {}
    tar_os = {}
    
    for cluster_id in tar_community_nodes:
        tar_node_count[cluster_id] = len(tar_community_nodes[cluster_id])
    sorted_tar_node_count = sorted(tar_node_count.iteritems(), key=operator.itemgetter(1))
    for i in range(0,len(sorted_tar_node_count)):
        tar_os[sorted_tar_node_count[i][0]] = i
    
    for cluster_id in ref_community_nodes:
        ref_node_count[cluster_id] = len(ref_community_nodes[cluster_id])
    sorted_ref_node_count = sorted(ref_node_count.iteritems(), key=operator.itemgetter(1))
    for i in range(0,len(sorted_ref_node_count)):
        ref_os[sorted_ref_node_count[i][0]] = i
    
    sim_table = {}
    for tar in tar_community_nodes.keys():
        sim_list = []
        for ref in ref_os.keys():
            score = -abs(tar_os[tar] - ref_os[ref])
            sim_list.append((ref,score))
        sim_list = sorted(sim_list, key=operator.itemgetter(1))
        sim_table[tar] = sim_list
    
    return sim_table, ref_community_nodes, tar_community_nodes

def cluster_id_sim(P_ref, P_tar):
    # match clusters by their id
    # used by kmeans_cumulative_partition
    
    ref_community_nodes = {}
    tar_community_nodes = {}
    for n in P_ref:
        ref_community_nodes.setdefault(P_ref[n],[]).append(n)
    for n in P_tar:
        tar_community_nodes.setdefault(P_tar[n],[]).append(n)

    sim_table = {}
    for tar in tar_community_nodes.keys():
        sim_list = []
        for ref in ref_community_nodes.keys():
            if tar == ref:
                score = 1
            else:
                score = 0
            sim_list.append((ref,score))
        sim_table[tar] = sim_list

    return sim_table, ref_community_nodes, tar_community_nodes

if __name__=='__main__':

    P_tar = {}
    P_tar[1] = 0
    P_tar[2] = 0
    P_tar[3] = 1
    P_tar[4] = 1
    P_tar[5] = 2
    P_ref = {}
    P_ref[1] = 0
    P_ref[2] = 0
    P_ref[3] = 1
    P_ref[4] = 1
    P_ref[5] = 2
    s, r, t = cluster_id_sim(P_ref, P_tar)
    print s
    print r
    print t

    
