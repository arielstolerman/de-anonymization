import conf
import os
import sys
import utility
import community_matcher

# set all configurations
# ======================

# graphs and feature tables
name_prefix = "./data/sms-wire-reduced/sms-wire-reduced"
conf.ref_graph_file = name_prefix+"-reference-graph.txt"
conf.target_graph_file = name_prefix+"-target-graph.txt"
conf.feature_names_file = name_prefix+"-features.txt"
conf.ref_feature_file = name_prefix+"-reference.txt"
conf.target_feature_file = name_prefix+"-target.txt"

nodes_pool_ratio_start = 0.05
nodes_pool_ratio_end = 0.5
nodes_pool_ratio_step = 0.15

# original options:
# =================
optional_partition_method = ["louvain","metis","kmeans_features","kmeans_cumulative_features"]
optional_num_of_clusters = [10,100,1000]
optional_cluster_sim_measure = ["netsimile","cluster_rank"] # use "cluster_id" for "kmeans_cumulative_features"

# run all configurations
# ======================
tmp = []
run_num = 1
for i in range(len(optional_partition_method)):
    # set partition method
    conf.partition_method = optional_partition_method[i]

    # mark num of clusters as irrelevant for louvain
    if conf.partition_method == "louvain":
        tmp = optional_num_of_clusters[:]
        optional_num_of_clusters = [-1]

    # restrict num of clusters to <= 10 for metis
    if conf.partition_method == "metis":
        tmp = optional_num_of_clusters[:]
        optional_num_of_clusters = []
        for elem in tmp:
            if elem <= 10:
                optional_num_of_clusters.append(elem)

    # restrict cluster_sim_measure to "cluster_id" for kmeans_cumulative_features
    if conf.partition_method == "kmeans_cumulative_features":
        tmp = optional_cluster_sim_measure[:]
        optional_cluster_sim_measure = ["cluster_id"]

    for j in range(len(optional_num_of_clusters)):
        # set num of clusters, except for louvain
        conf.num_of_clusters = optional_num_of_clusters[j]

        for k in range(len(optional_cluster_sim_measure)):
            # set cluster similarity measurement method
            conf.cluster_sim_measure = optional_cluster_sim_measure[k]
            nodes_pool_ratio = nodes_pool_ratio_start

            while nodes_pool_ratio <= nodes_pool_ratio_end:
                conf.nodes_pool_ratio = nodes_pool_ratio
                num_clusters_string = ""
                if conf.partition_method != 'louvain':
                    num_clusters_string = "_num-clusters-"+str(conf.num_of_clusters)
                log_name = name_prefix+"_community_log_"+str(run_num)+"_par-method-"+conf.partition_method+num_clusters_string+"_sim-"+conf.cluster_sim_measure+"_npr-"+str(nodes_pool_ratio)+".txt"

                # ===== run =====
                print "Run "+str(run_num)+":"
                print "- partition method: "+conf.partition_method
                print "- number of clusters: "+str(conf.num_of_clusters)
                print "- cluster similarity measurement: "+conf.cluster_sim_measure
                print "- nodes pool ratio: "+str(conf.nodes_pool_ratio)
                print "- log filename: "+log_name

                os.system("nohup python community_matcher.py 1> "+log_name+
                          " "+conf.ref_graph_file+
                          " "+conf.ref_feature_file+
                          " "+conf.target_graph_file+
                          " "+conf.target_feature_file+
                          " "+str(conf.nodes_pool_ratio)+
                          " "+conf.partition_method+
                          " "+str(conf.num_of_clusters)+
                          " "+conf.cluster_sim_measure+
                          '')
                
                # use this for straight-forward
                #utility.statistics()
                #community_matcher.community_matcher()
                
                print "done!"
                print ""

                nodes_pool_ratio += nodes_pool_ratio_step
                run_num += 1

    if conf.partition_method in ["louvain","metis"]:
        optional_num_of_clusters = tmp[:]
    if conf.partition_method == "kmeans_cumulative_features":
        optional_cluster_sim_measure = tmp[:]

