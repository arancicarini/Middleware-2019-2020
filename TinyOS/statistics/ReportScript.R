library(ggplot2)


topology=c(7,15,31, 63)
DataMessageTime.avg=c(1.053,  1.438, 1.690,2.191)
df = cbind(topology, DataMessageTime.avg)
df = as.data.frame(df)

ggplot(df, aes(x  =  topology,
      y    =  DataMessageTime.avg)) + 
      geom_point() +
      geom_smooth(method = "lm", se = FALSE, col = "black") + labs(x ="number of nodes in the network",
                                                    y = "avg time to deliver a data message [ms]", 
                                                    title = "Tree topologies")

topology=c(7,15,31, 63)
DataMessageTime.avg=c(1.053/7,  1.438/15, 1.670/31,2.190/63)
df = cbind(topology, DataMessageTime.avg)
df = as.data.frame(df)

ggplot(df, aes(x  =  topology,
               y    =  DataMessageTime.avg)) + 
  geom_point() +
  geom_smooth(method = "lm", se = FALSE, col = "black") + labs(x ="number of nodes in the network",
                                                               y = "normalized avg time to deliver a data message [ms]", 
                                                               title = "Tree topologies")

topology=c(7,15,31, 63)
TresholdMessageTime.avg=c(0.958,  1.362, 1.867,2.861)
df = cbind(topology, TresholdMessageTime.avg)
df = as.data.frame(df)

ggplot(df, aes(x  =  topology,
               y    =  TresholdMessageTime.avg)) + 
  geom_point() +
  geom_smooth(method = "lm", se = FALSE, col = "black") + labs(x ="number of nodes in the network",
                                                               y = "avg time to deliver a treshold message [ms]", 
                                                               title = "Tree topologies")

topology=c(7,15,31, 63)
TresholdMessageTime.avg=c(0.958/7,  1.362/15, 1.867/31,2.861/63)
df = cbind(topology, TresholdMessageTime.avg)
df = as.data.frame(df)

ggplot(df, aes(x  =  topology,
               y    =  TresholdMessageTime.avg)) + 
  geom_point() +
  geom_smooth(method = "lm", se = FALSE, col = "black") + labs(x ="number of nodes in the network",
                                                               y = "normalized avg time to deliver a treshold message [ms]", 
                                                               title = "Tree topologies")

topology=c(7,15,31, 63)
count=c(121, 156, 209,283)
df = cbind(topology, count)
df = as.data.frame(df)

ggplot(df, aes(x  =  topology,
               y    =  count)) + 
  geom_point() +
  geom_smooth(method = "lm", se = FALSE, col = "black") + labs(x ="number of nodes in the network",
                                                               y = "total amount of exchanged messages", 
                                                               title = "Tree topologies")

topology=c(7,15,31, 63)
count=c(121/7, 156/15, 209/31,283/63)
df = cbind(topology, count)
df = as.data.frame(df)

ggplot(df, aes(x  =  topology,
               y    =  count)) + 
  geom_point() +
  geom_smooth(method = "lm", se = FALSE, col = "black") + labs(x ="number of nodes in the network",
                                                               y = "normalized total amount of exchanged messages", 
                                                               title = "Tree topologies")


topology=c(7,15,31, 63)
time=c(43.99, 38.15, 36.19,35.23)
df = cbind(topology, time)
df = as.data.frame(df)

ggplot(df, aes(x  =  topology,
               y    =  time)) + 
  geom_point() +
  geom_smooth(method = "lm", se = FALSE, col = "black") + labs(x ="number of nodes in the network",
                                                               y = "total simulation time [s]", 
                                                            title = "Tree topologies")

## comparisons## 
topology=c("31","5.31","3.40" )
DataMessageTime.avg=c(1.690,1.128, 1.487)
df = cbind(topology, DataMessageTime.avg)
df = as.data.frame(df)

ggplot(df, aes(x  =  topology,
               y    =  DataMessageTime.avg)) + 
   geom_bar(stat = "identity", position = position_dodge())+
  labs(x ="topologies",
      y = "avg time to deliver a data message [ms]", 
     title = "Topologies comparison: tree vs ternary tree vs star ")

topology=c("31","5.31","3.40")
DataMessageTime.avg=c(1.690/31,1.128/31, 1.487/40)
df = cbind(topology, DataMessageTime.avg)
df = as.data.frame(df)

ggplot(df, aes(x  =  topology,
               y    =  DataMessageTime.avg)) + 
  geom_bar(stat = "identity", position = position_dodge())+
  labs(x ="topologies",
       y = "normalized avg time to deliver a data message [ms]", 
       title = "Topologies comparison: tree vs ternary tree vs star ")

topology=c("31","3.40", "5.31")
TresholdMessageTime.avg=c(1.867, 1.806,1.463)
df = cbind(topology, TresholdMessageTime.avg)
df = as.data.frame(df)

ggplot(df, aes(x  =  topology,
               y    =  TresholdMessageTime.avg)) + 
  geom_bar(stat = "identity", position = position_dodge())+
  labs(x ="topologies",
       y = "avg time to deliver a treshold message [ms]", 
       title = "Topologies comparison: tree vs ternary tree vs star ")

topology=c("31"," 5.31","3.40")
TresholdMessageTime.avg=c(1.867/31,1.463/31, 1.806/40)
df = cbind(topology, TresholdMessageTime.avg)
df = as.data.frame(df)

ggplot(df, aes(x  =  topology,
               y    =  TresholdMessageTime.avg)) + 
  geom_bar(stat = "identity", position = position_dodge())+
  labs(x ="topologies",
       y = "normalized avg time to deliver a treshold message [ms]", 
       title = "Topologies comparison: tree vs ternary tree vs star ")

topology=c("31","5.31","3.40" )
count=c(209,202, 256)
df = cbind(topology, count)
df = as.data.frame(df)

ggplot(df, aes(x  =  topology,
               y    =  count)) + 
  geom_bar(stat = "identity", position = position_dodge())+
  labs(x ="topologies",
       y = "total amount of exchanged messages", 
       title = "Topologies comparison: tree vs ternary tree vs star ")

topology=c("31","3.40", "5.31")
count=c(209/31, 256/40,202/31)
df = cbind(topology, count)
df = as.data.frame(df)

ggplot(df, aes(x  =  topology,
               y    =  count)) + 
  geom_bar(stat = "identity", position = position_dodge())+
  labs(x ="topologies",
       y = "normalized total amount of exchanged messages", 
       title = "Topologies comparison: tree vs ternary tree vs star ")

topology=c("31","5.31","3.40" )
time=c(36.19,38.13, 37.17)
df = cbind(topology, time)
df = as.data.frame(df)

ggplot(df, aes(x  =  topology,
               y    =  time)) + 
  geom_bar(stat = "identity", position = position_dodge())+
  labs(x ="topologies",
       y = "total simulation time [s]", 
       title = "Topologies comparison: tree vs ternary tree vs star ")




