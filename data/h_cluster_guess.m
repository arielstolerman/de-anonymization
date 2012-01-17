A = load('ego_table.txt');

A = load('yahoo_pr.txt');
good_guess = A(:,1);
A = A(:,2:9);

names = num2str( good_guess);

Z = linkage(A,'single','euclidean');
[H,T,P]=dendrogram(Z,0,'labels', names);

for i = 0:9
    i
    sum(good_guess(P(i*100+1:(i+1)*100)))
    sum(good_guess(i*100+1:(i+1)*100))
end





sum(good_guess(P(440:540)))
sum(good_guess(440:540))


