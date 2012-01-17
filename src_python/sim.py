import numpy 

def compute_distance( m=1 ,v=1,T=1,S=1,D=1, method='euclidean'):
    #m is feature matrix for reference graph.
    #v is the feature of the node in target graph
    #T, S, D is the decomposition results for m.
    #method is parameter for chosing which distance metric to use
    if(type(m)==list):
        m = numpy.array(m);
    if(type(v)==list):
        v = numpy.array(v);

    if(method=='euclidean'):
        r = numpy.sum(((m-v)**2), axis=1);
        return r
    if(method=='euclidean_svd'):
        svd_target_node_feature = numpy.dot(numpy.dot(v, D.T), numpy.linalg.inv(numpy.diag(S)));
        r = numpy.sqrt(numpy.sum((T-svd_target_node_feature)**2, axis=1))
        return r
    #not implemented yet
    #if(method=='cosine'):
    #    r = 1-numpy.sum((T*target_node_feature),axis=1)/(numpy.sqrt(numpy.sum(T**2))*numpy.sqrt(numpy.sum(target_node_feature**2))+0.00001);
    #    return sorted(zip( range(0,len(r)), list(r)), key=operator.itemgetter(1));
