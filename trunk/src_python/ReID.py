#copy right, Ke Xie, Rutgers University, 2011
#all rights reservered


from sim import compute_distance
from sparsesvd import sparsesvd
import numpy, scipy.sparse
import operator
import utility
import conf
import sys

def read_data():
    print "reading feature tables"
    top_nodes = utility.find_top_degree_nodes();
    ref_feature = utility.read_feature_table(conf.ref_feature_file);
    target_feature = utility.read_feature_table(conf.target_feature_file);
    ref_keys = sorted(ref_feature.keys());
    target_keys = sorted(target_feature.keys());
    ref_size = len(ref_keys)
    G_tar = utility.open_graph(conf.target_graph_file);
    G_ref = utility.open_graph(conf.ref_graph_file);
    for t in target_feature.keys():
        if(t not in top_nodes ):
            del target_feature[t]
    target_keys = target_feature.keys()
    return top_nodes,ref_feature,target_feature,ref_keys,target_keys,ref_size,G_tar,G_ref,conf.percentage

def find_candidates():
    top_nodes, ref_feature, target_feature, ref_keys, target_keys, ref_size, G_tar, G_ref, percentage = read_data()
    ref_matrix = numpy.zeros( (len(ref_keys), len(ref_feature[ref_keys[0]])) );
    for i in xrange(len(ref_keys)):
        ref_matrix[i] = ref_feature[ref_keys[i]];
    ref_matrix = ref_matrix.astype(numpy.int32);#for speed concern, don't use 64 int
    
    if(conf.similarity_measure.find('svd')!=-1):
        print "Begin decompose ref_feature matrix"
        #decompose the reference matrix
        T,S,D = sparsesvd(scipy.sparse.csc_matrix(ref_matrix),conf.factor);
        T = T.T;
        S = numpy.sqrt(S)
        print "Decomposition done"
    good_guess = 0
    sum_score = 0;
    top_nodes = list(top_nodes)
    for i in range(len(top_nodes)):
        current_node = top_nodes[i]
        target_node_feature = numpy.array(target_feature[current_node])
        print 'working on '+str(i)+'th node in target graph'
        if conf.similarity_measure=='euclidean_svd':
            similarity_scores = compute_distance(v = target_node_feature, T = T, S = S, D = D, method=conf.similarity_measure)
        elif conf.similarity_measure=='euclidean':
            similarity_scores = compute_distance(ref_matrix, target_node_feature, method=conf.similarity_measure)
        
        w = sorted(zip(ref_keys, similarity_scores), key=operator.itemgetter(1,0));
        score = 0;
        for j in xrange(len(w)):
            score+=1;
            if(w[j][0]==current_node):
                if(score*1.0/ref_size<=percentage):
                    good_guess+=1;
                    print "Now good guess is %d, score is %d %s"%(good_guess,score,current_node)
                    break
        sum_score+=score
    utility.report_printer(sum_score, good_guess)
if __name__=='__main__':
    #ref_graph, ref_feature, tar_graph, target_feature
    
    args = sys.argv[1:]
    
    if(len(args)==4):
        conf.ref_graph_file = args[0]
        conf.ref_feature_file = args[1]
        conf.target_graph_file = args[2]
        conf.target_feature_file = args[3]
    utility.statistics()
    find_candidates()

