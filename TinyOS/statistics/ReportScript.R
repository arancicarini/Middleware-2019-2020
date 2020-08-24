library(ggplot2)

topology=c("07","15","31", "63","3.40","5.31")
DataMessageTime.avg=c(45.163,  22.51, 7.31437,15.6461,10.025,7.82304, 0, 2, 0.837)

qplot(x    =  numLayers,
      y    =  numRequests.mean,
) +
  
  geom_errorbar(aes(ymin = numRequests.mean - sd,
                    ymax = numRequests.mean + sd,
                    width = 0.10))
numRequests=c("06","08", "10","12","14","16","18","20")
mean=c(  801.2,  377,  284.6,  132,  51.2,  10,  2,  0.8)
sd=c(  22.51, 7.31437,15.6461,10.025,7.82304, 0, 2, 0.837)
qplot(x    =  numRequests,
      y    =  mean,
) +
  
  geom_errorbar(aes(ymin = mean - sd,
                    ymax = mean + sd,
                    width = 0.10))

