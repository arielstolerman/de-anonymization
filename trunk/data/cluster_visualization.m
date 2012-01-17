A = load('cluster.txt');
[names]=textread('label.txt','%s%*[^\n]');

A = load('ego_table.txt');
good_guess = A(:,1);
A = A(:,2:5);

names = num2str( good_guess);

Z = linkage(A,'single','euclidean');
[H,T,P]=dendrogram(Z,0,'labels', names);


for i=1:length(P)
    text(-0.8, i, 'ha');
end