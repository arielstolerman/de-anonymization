import conf
import os
import sys
graph_data_prefix = "./data/yahoo/yahoo_d"
feature_data_prefix = "./data/yahoo/yahoo-d00-d"
ref_feature_data_suffix = "-reference.txt"
tar_feature_data_suffix = "-target.txt"

args = sys.argv[1:]
if(len(args)!=2):
    print "wrong command"
    print """use 
    python run_community start end
    to indicate start from which day and ends at which day
    """
    exit(2)
start = (int)(args[0])
end = (int)(args[1])


#change setting here use 
# conf.some_attribute = some_value

for i in range(start,end):
    day_tar = "00"
    if(i>=10):
        day_ref = str(i)
    else:
        day_ref = "0"+str(i)
    conf.ref_graph_file = graph_data_prefix+day_ref+".txt"
    conf.target_graph_file = graph_data_prefix+day_tar+".txt"
    conf.ref_feature_file = feature_data_prefix+day_ref+ref_feature_data_suffix
    conf.target_feature_file = feature_data_prefix+day_ref+tar_feature_data_suffix
    
    #change here to see what ratio can produce good results. you could use a loop and iterate nodes_pool_ratio from 0.05 to 0.5 But when
    #you are using loop, be sure your machine will not crash because of out of memory. a sinlge process will take 3G of memory. (for ratio = 0.05
    #it takes 3G, and for 0.10 it takes 3G...so you could only run a pair at a time on your 64G machine. which means your command should be like
    #"python run_community.py start end" and end-start<=3. or your machine will surely down.
    #first just let's iterate ratio. and after that you could modify this part and use metis or rank_similarity
    
    
    conf.nodes_pool_ratio = 0.05
    while(conf.nodes_pool_ratio<=0.5):
        #conf.nodes_pool_ratio = 0.05
        os.system('nohup python community_matcher.py 1> yahoo_day_community_log_'+day_ref+"_"+str(conf.nodes_pool_ratio)+
                  " "+conf.ref_graph_file+
                  " "+conf.ref_feature_file+
                  " "+conf.target_graph_file+
                  " "+conf.target_feature_file+
                  " "+str(conf.nodes_pool_ratio)+
                  " "+conf.partition_method+
                  " "+str(conf.num_of_clusters)+
                  " "+conf.cluster_sim_measure+
                  ' &')
        conf.nodes_pool_ratio+=0.05
