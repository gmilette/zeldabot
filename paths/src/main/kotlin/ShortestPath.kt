class ShortestPath {
//    https://levelup.gitconnected.com/dijkstras-shortest-path-algorithm-in-a-grid-eb505eb3a290
    /**
    #Initialize auxiliary arrays
    distmap=np.ones((max_val,max_val),dtype=int)*np.Infinity
    distmap[0,0]=0
    originmap=np.ones((max_val,max_val),dtype=int)*np.nan
    visited=np.zeros((max_val,max_val),dtype=bool)
    finished = False
    x,y=np.int(0),np.int(0)
    count=0

    #Loop Dijkstra until reaching the target cell
    while not finished:
    # move to x+1,y
    if x < max_val-1:
    if distmap[x+1,y]>map[x+1,y]+distmap[x,y] and not visited[x+1,y]:
    distmap[x+1,y]=map[x+1,y]+distmap[x,y]
    originmap[x+1,y]=np.ravel_multi_index([x,y], (max_val,max_val))
    # move to x-1,y
    if x>0:
    if distmap[x-1,y]>map[x-1,y]+distmap[x,y] and not visited[x-1,y]:
    distmap[x-1,y]=map[x-1,y]+distmap[x,y]
    originmap[x-1,y]=np.ravel_multi_index([x,y], (max_val,max_val))
    # move to x,y+1
    if y < max_val-1:
    if distmap[x,y+1]>map[x,y+1]+distmap[x,y] and not visited[x,y+1]:
    distmap[x,y+1]=map[x,y+1]+distmap[x,y]
    originmap[x,y+1]=np.ravel_multi_index([x,y], (max_val,max_val))
    # move to x,y-1
    if y>0:
    if distmap[x,y-1]>map[x,y-1]+distmap[x,y] and not visited[x,y-1]:
    distmap[x,y-1]=map[x,y-1]+distmap[x,y]
    originmap[x,y-1]=np.ravel_multi_index([x,y], (max_val,max_val))

    visited[x,y]=True
    dismaptemp=distmap
    dismaptemp[np.where(visited)]=np.Infinity
    # now we find the shortest path so far
    minpost=np.unravel_index(np.argmin(dismaptemp),np.shape(dismaptemp))
    x,y=minpost[0],minpost[1]
    if x==max_val-1 and y==max_val-1:
    finished=True
    count=count+1

    #Start backtracking to plot the path
    mattemp=map.astype(float)
    x,y=max_val-1,max_val-1
    path=[]
    mattemp[np.int(x),np.int(y)]=np.nan

    while x>0.0 or y>0.0:
    path.append([np.int(x),np.int(y)])
    xxyy=np.unravel_index(np.int(originmap[np.int(x),np.int(y)]), (max_val,max_val))
    x,y=xxyy[0],xxyy[1]
    mattemp[np.int(x),np.int(y)]=np.nan
    path.append([np.int(x),np.int(y)])

    #Output and visualization of the path
    current_cmap = plt.cm.Blues
    current_cmap.set_bad(color='red')
    fig, ax = plt.subplots(figsize=(8,8))
    ax.matshow(mattemp,cmap=plt.cm.Blues, vmin=0, vmax=20)
    for i in range(max_val):
    for j in range(max_val):
    c = map[j,i]
    ax.text(i, j, str(c), va='center', ha='center')

    print('The path length is: '+np.str(distmap[max_val-1,max_val-1]))
    print('The dump/mean path should have been: '+np.str(maxnum*max_val))
     */
}