import conf
import os

graph_data_prefix = "./data/yahoo/yahoo_d"
feature_data_prefix = "./data/yahoo/yahoo-d00-d"
ref_feature_data_suffix = "-reference.txt"
tar_feature_data_suffix = "-target.txt"


for i in range(1,28):
    day_tar = "00"
    if(i>=10):
        day_ref = str(i)
    else:
        day_ref = "0"+str(i)
    conf.ref_graph_file = graph_data_prefix+day_ref+".txt"
    conf.target_graph_file = graph_data_prefix+day_tar+".txt"
    conf.ref_feature_file = feature_data_prefix+day_ref+ref_feature_data_suffix
    conf.target_feature_file = feature_data_prefix+day_ref+tar_feature_data_suffix
    """
    if(conf.percentage!=0.01):
        print "Are you sure you want to use conf.guess_range as %f ?"%conf.percentage
        print "(y/n)"
        ans = raw_input()
        if(ans=='y'):
            pass
        else:
            sys.exit(2)
    """
    #utility.report_printer(0,0)
    os.system('nohup python ReID.py 1> yahoo_day_'+day_ref+" "+conf.ref_graph_file+" "+conf.ref_feature_file+" "+conf.target_graph_file+" "+conf.target_feature_file+' &')   
    
