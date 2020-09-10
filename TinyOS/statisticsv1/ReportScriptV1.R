library(ggplot2)


## comparisons## 
topology=c("14 dense","14 sparse","random", "mix", "tree", "20 dense" )
DiscardedMessages.avg=c(14.5, 6.17, 22.67,34.33, 5.33, 28.0)
df = cbind(topology, DiscardedMessages.avg)
df = as.data.frame(df)
df$DiscardedMessages.avg = as.numeric(as.character(df$DiscardedMessages.avg))
ggplot(df, aes(x  =  topology,
               y    =  DiscardedMessages.avg)) + 
  geom_bar(stat = "identity", position = position_dodge())+
  labs(x ="topologies",
       y = "avg number of discarded setup messages", 
       title = "Topologies comparison")


topology=c("14 dense","14 sparse","random", "mix", "tree", "20 dense" )
ConvergenceTime.avg=c(2.85,2.85, 3.32,3.93,2.73,3.54)
df = cbind(topology, ConvergenceTime.avg)
df = as.data.frame(df)
df$ConvergenceTime.avg = as.numeric(as.character(df$ConvergenceTime.avg))
ggplot(df, aes(x  =  topology,
               y    =  ConvergenceTime.avg)) + 
  geom_bar(stat = "identity", position = position_dodge())+
  labs(x ="topologies",
       y = "avg convergence time of setup messages[ms]", 
       title = "Topologies comparison")

topology=c("14 dense","14 sparse","random", "mix", "tree", "20 dense" )
DataMessageTime.avg=c(1.18,1.40, 1.19,1.32,1.55,0.99)
df = cbind(topology, DataMessageTime.avg)
df = as.data.frame(df)
df$DataMessageTime.avg = as.numeric(as.character(df$DataMessageTime.avg))
ggplot(df, aes(x  =  topology,
               y    =  DataMessageTime.avg)) + 
  geom_bar(stat = "identity", position = position_dodge())+
  labs(x ="topologies",
       y = "avg time to deliver a data message [ms]", 
       title = "Topologies comparison")


topology=c("14 dense","14 sparse","random", "mix", "tree", "20 dense" )
TresholdMessageTime.avg=c(1.50, 1.34,1.40,1.88,1.52,1.38)
df = cbind(topology, TresholdMessageTime.avg)
df = as.data.frame(df)
df$TresholdMessageTime.avg = as.numeric(as.character(df$TresholdMessageTime.avg))
ggplot(df, aes(x  =  topology,
               y    =  TresholdMessageTime.avg)) + 
  geom_bar(stat = "identity", position = position_dodge())+
  labs(x ="topologies",
       y = "avg time to deliver a treshold message [ms]", 
       title = "Topologies comparison")


topology=c("14 dense","14 sparse","random", "mix", "tree", "20 dense" )
count=c(66,46,73,78,46,105)
df = cbind(topology, count)
df = as.data.frame(df)
df$count = as.numeric(as.character(df$count))
ggplot(df, aes(x  =  topology,
               y    =  count)) + 
  geom_bar(stat = "identity", position = position_dodge())+
  labs(x ="topologies",
       y = "total amount of not discarded threshold messages", 
       title = "Topologies comparison")


topology=c("14 dense","14 sparse","random", "mix", "tree", "20 dense" )
count=c(9,14,10,12,18,12)
df = cbind(topology, count)
df = as.data.frame(df)
df$count = as.numeric(as.character(df$count))
ggplot(df, aes(x  =  topology,
               y    =  count)) + 
  geom_bar(stat = "identity", position = position_dodge())+
  labs(x ="topologies",
       y = "total amount of not sent messages due to a busy radio", 
       title = "Topologies comparison")






