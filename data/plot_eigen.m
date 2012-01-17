A = load('cluster.txt');
names=textread('label.txt','%s%*[^\n]');
[U,S,V] = svds(A,6);

x = U(:,5);
y = U(:,6);

%x = x(1:50,:);
%y = y(1:50,:);
cnt = zeros(6,1);

only_plot = ['vldb','cikm']


for i = 1:length(x)
    i
    t = names{i};
    
    if( isempty(strfind(only_plot, t(1:3))))
        continue
    end
    
    if( ~isempty(strfind(t,'kdd')) )
        h1=plot(x(i),y(i),'b.');
        cnt(1) = cnt(1)+1;
        %h1=scatter(x(i),y(i),50,'b.');
    elseif(~isempty(strfind(t,'sdm')))
        h2=plot(x(i),y(i),'g*');
        cnt(2) = cnt(2)+1;
        %h2=scatter(x(i),y(i),50,'g*');
    elseif(~isempty(strfind(t,'vldb')))
        %h3=plot(x(i),y(i),'rs');
        h3 = plot(x(i),y(i),'b.');
        cnt(3) = cnt(3)+1;
        %h3=scatter(x(i),y(i),50,'rs');
    elseif(~isempty(strfind(t,'sigmod')))
        h4=plot(x(i),y(i),'cd');
        cnt(4) = cnt(4)+1;
        %h4=scatter(x(i),y(i),50,'cd');
    elseif(~isempty(strfind(t,'cikm')))
        %h5=plot(x(i),y(i),'yo');
        h5= plot(x(i),y(i),'g*');
        cnt(5) = cnt(5)+1;
        %h5=scatter(x(i),y(i),50,'yo');
    elseif(~isempty(strfind(t,'icdm')))
        %h6=plot(x(i),y(i),'kp');
        cnt(6) = cnt(6)+1; 
        %h6=scatter(x(i),y(i),50,'kp');

    end 
    hold on
end
%H = [h1,h2,h3,h4,h5,h6];
H = [h3,h5];
%legend(H, 'kdd','sdm','vldb','sigmod','cikm','icdm');
legend(H,'vldb','cikm');
cnt