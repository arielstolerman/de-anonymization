#global varibles
#these variables are used to contain the global information
n_ref_partition = -1
n_tar_partition = -1
n_ref_nodes = -1
n_tar_nodes = -1
n_ref_edges = -1
n_tar_edges = -1
n_common_nodes = -1

#global
undirected = True
weighted = True

# guess range
percentage = 0.01

# how many top degrees
n_top_degree_nodes = 1000000

# partition method:
# graph partitioning options: "louvain", "metis"
# feature table partitioning options: "kmeans_features", "kmeans_cumulative_features"
partition_method = "kmeans_cumulative_features"

# any metis arguments to go into gpmetis
metis_args = ""

# number of clusters to partition into (relevant only for some partitioning methods)
num_of_clusters = 10

# cluster similarity method
# choose from: "netsimile", "cluster_rank"
cluster_sim_measure = "netsimile"


###parameters for ReID ###
similarity_measure = 'euclidean'
#if you want to use 'svd_euclidean', you should specify your factor, or it will be 7
#similarity_measure = 'euclidean_svd'
factor = 7
###parameters for ReID ###

###community based begins###
nodes_pool_ratio = 0.1
###community based ends###


#at&t data here
#ref_graph_file = "../data/allawsv-anon.2011-09"
#target_graph_file = "../data/allsms-anon.2011-09"
#ref_feature_file = "../data/sms_wire-reference.txt"
#target_feature_file = "../data/sms_wire-target.txt"

#ref_graph_file = "../data/yahoo/yahoo-d00-d01-reference-graph.txt"
#target_graph_file = "data/yahoo/yahoo-d00-d01-target-graph.txt"
#feature_names_file = "../data/yahoo/yahoo-d00-d01-features.txt"
#ref_feature_file = "../data/yahoo/yahoo-d00-d01-reference.txt"
#target_feature_file = "../data/yahoo/yahoo-d00-d01-target.txt"

ref_graph_file = '../data/d-sdm.csv'
ref_feature_file='../data/kdd_sdm-reference.txt'
target_graph_file='../data/d-sigkdd.csv'
target_feature_file = '../data/kdd_sdm-target.txt'

#target_graph_file = '../data/d-vldb.csv'
#target_feature_file = '../data/vldb_sigmod-target.txt'
#ref_graph_file = '../data/d-sigmod.csv'
#ref_feature_file = '../data/vldb_sigmod-reference.txt'

#ref_graph_file = "../data/d-cikm.csv"
#ref_feature_file = "../data/kdd_cikm-reference.txt"
#target_graph_file = "../data/d-sigkdd.csv"
#target_feature_file = "../data/kdd_cikm-target.txt"

#target_graph_file = "../data/d-cikm.csv"
#target_feature_file = "../data/cikm_icdm-reference.txt"
#ref_graph_file = "../data/d-icdm.csv"
#ref_feature_file = "../data/cikm_icdm-target.txt"

#ref_graph_file =    "../data/d-sigkdd.csv" 
#ref_feature_file =  "../data/co_author-reference.txt"
#target_graph_file = "../data/d-icdm.csv"
#target_feature_file = "../data/co_author-target.txt" 

# all new yahoo data
####################
# d00-d01
#ref_graph_file = "../data/yahoo/yahoo-d00-d01-reference-graph.txt"
#target_graph_file = "data/yahoo/yahoo-d00-d01-target-graph.txt"
#feature_names_file = "../data/yahoo/yahoo-d00-d01-features.txt"
#ref_feature_file = "../data/yahoo/yahoo-d00-d01-reference.txt"
#target_feature_file = "../data/yahoo/yahoo-d00-d01-target.txt"
