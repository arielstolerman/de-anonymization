# imports
import conf
import community
import utility
import os
import numpy
from scipy.cluster.vq import *

# main method for partitioning according to settings in conf.py
# returns a dictionary of nodes to clusters for the given graph filename
def partition(tar_graph_name, tar_graph, tar_feature_table, ref_graph_name, ref_graph, ref_feature_table):
    if(conf.partition_method == "louvain"):
        print "partitioning target graph..."
        tar = louvain_partition(tar_graph)
        print "done!"
        print "partitioning reference graph..."
        ref = louvain_partition(ref_graph)
        print "done!"
        return tar, ref
    elif (conf.partition_method == "metis"):
        print "partitioning target graph..."
        tar = metis_partition(tar_graph_name)
        print "done!"
        print "partitioning reference graph..."
        ref = metis_partition(ref_graph_name)
        print "done!"
        return tar, ref
    elif (conf.partition_method == "kmeans_features"):
        print "partitioning target graph..."
        tar = kmeans_partition(tar_graph, tar_feature_table)
        print "done!"
        print "partitioning reference graph..."
        ref = kmeans_partition(ref_graph, ref_feature_table)
        print "done!"
        return tar, ref
    elif (conf.partition_method == "kmeans_cumulative_features"):
        return kmeans_cumulative_partition(tar_graph, tar_feature_table, ref_graph, ref_feature_table)


###################################
# partitioning applied on the graph
###################################

# default networkx partitioning using the Louvain algorithm
def louvain_partition(graph):
	return community.best_partition(graph)
    #return community.best_partition(utility.open_graph(graph_name))
    #return community.best_partition(utility.open_graph(graph_name).to_undirected())

# using METIS for partitioning
def metis_partition(graph_name):
	s_num_clusters = str(conf.num_of_clusters)
	
	# create metis graph
	print "phase 1/3: creating METIS graph for "+graph_name
	command = "java -jar metis.jar create_metis_files "+graph_name+" "+s_num_clusters
	print "running: "+command
	os.system(command)
	metis_graph_name = graph_name+"_metis.txt"
	
	# run metis
	print "phase 2/3: running gpmetis for partitioning"
	command = "gpmetis "+metis_graph_name+" "+s_num_clusters+" "+conf.metis_args
	print "runnning: "+command
	os.system(command)
	
	# create dictionary
	print "phase 3/3: creating mapping from nodes to clusters"
	command = "java -jar metis.jar create_cluster_map "+graph_name+" "+s_num_clusters
	print "running: "+command
	os.system(command)
	metis_cluster_map = graph_name+"_metis_nodes_to_clusters_map_"+s_num_clusters+".txt"
	f = open(metis_cluster_map, 'r')
	line = f.readline()
	dic = {}
	while line:
		key, value = line.split(',')
		dic[key] = int(value.strip())
		line = f.readline()
	f.close()
	return dic

###########################################
# partitioning applied on the feature table
###########################################

# k-means based clustering
def kmeans_partition(graph, feature_table):
    feature_matrix = numpy.array(feature_table.values())
    centroid, label = kmeans2(feature_matrix, conf.num_of_clusters)
    dic = {}
    keys = feature_table.keys()
    for i in range(0,len(feature_table)):
        dic[keys[i]] = label[i]
    return dic

def kmeans_cumulative_partition(tar_graph, tar_feature_table, ref_graph, ref_feature_table):
    tar_dic = {}
    ref_dic = {}
    tar_prefix = "tar_"
    ref_prefix = "ref_"

    # combine feature tables
    all_feature_table = {}
    for tar_key in tar_feature_table.keys():
        all_feature_table[tar_prefix+str(tar_key)] = tar_feature_table[tar_key]
    for ref_key in ref_feature_table.keys():
        all_feature_table[ref_prefix+str(ref_key)] = ref_feature_table[ref_key]

    # partition cumulative feature table and build cluster mappings
    feature_matrix = numpy.array(all_feature_table.values())
    centroid, label = kmeans2(feature_matrix, conf.num_of_clusters)
    keys = all_feature_table.keys()
    for i in range(0,len(all_feature_table)):
        key = keys[i]
        # target key
        if key.startswith(tar_prefix):
            tar_dic[key.split(tar_prefix)[1]] = label[i]
        # reference key
        elif key.startswith(ref_prefix):
            ref_dic[key.split(ref_prefix)[1]] = label[i]
    
    return tar_dic, ref_dic


if __name__=='__main__':
    G_ref = utility.open_graph(conf.ref_graph_file);
    G_tar = utility.open_graph(conf.target_graph_file);
    ref_feature_table = utility.read_feature_table(conf.ref_feature_file)
    tar_feature_table = utility.read_feature_table(conf.target_feature_file)
    
    #Partition here
    print "Partitioning graphs..."
    P_tar, P_ref = partition(conf.target_graph_file,G_tar,tar_feature_table,
                                       conf.ref_graph_file,G_ref,ref_feature_table)
