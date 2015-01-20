##
# Author: seninp
# modified: 12-2013
##
## get connection
require(RMySQL)
require(ggplot2)
require(reshape)
require(scales)
require(gridExtra)
require(Cairo)
#
require(splines)
require(MASS)
require(lubridate)
library(zoo)

releases=read.table("../resources/releases.csv",as.is=T,sep=",")
names(releases)=c("id","date")
str(releases)
releases$date=as.POSIXct(releases$date)

releases$prestart=releases$date-days(31)
releases$preend=releases$date-days(3)
releases$poststart=releases$date+days(3)
releases$postend=releases$date+days(31)


#
# connect to DB
# 
con <- dbConnect(MySQL(), user="postgre", password="postgre", dbname="postgre2", host="localhost")
#
# COMMITS HISTORY, qery DB function
#
res <- dbGetQuery(con, 
        paste("select sum(c.added_lines) added_loc, ",
        "DATE_FORMAT(c.utc_time,'%Y-%m-%d') week from `change` c group by week",sep=""))
res$week = as.POSIXlt(res$week)

ts = zoo(res$added_loc, res$week)
ts_smooth <- rollmean(ts, 24, fill = list(mean(ts), NULL, mean(ts)))
res$smooth = coredata(ts_smooth)

total_loc_smooth <- ggplot(res, aes(week, smooth)) + geom_line() + theme_bw() +
  scale_x_datetime("Year", breaks=as.POSIXct(paste(c(1997:2013),"-01-01",sep="")),
    labels=c(1997:2013)) + stat_smooth() +
  scale_y_continuous("Added LOC") + 
  ggtitle("Daily new LOC dynamics (and the mean) in PostgreSQL")

total_loc_smooth = total_loc_smooth +  
  geom_rect(inherit.aes = FALSE , data=releases, 
            aes(xmin=prestart, xmax=preend,ymin=min(res$smooth),ymax=max(res$smooth)),
            fill=alpha("red",0.6))+
  geom_rect(inherit.aes = FALSE , data=releases, 
            aes(xmin=poststart, xmax=postend,ymin=min(res$smooth),ymax=max(res$smooth)),
            fill=alpha("blue",0.5)) +
  geom_vline(xintercept=as.numeric(releases$date), linetype=4, color=red) + theme_bw()
total_loc_smooth

Cairo(width = 900, height = 300, 
      file="figures/postgre_newloc_daily", 
      type="pdf", pointsize=12, 
      bg = "transparent", canvas = "white", units = "px", dpi = 82)
print(total_loc_smooth)
dev.off()



total_loc_smooth <- ggplot(res, aes(week, smooth)) + geom_line() + theme_bw() +
  scale_x_datetime("Year", limits=as.POSIXct(c("2005-01-01", "2010-01-01"))) +
  scale_y_continuous("Added LOC") + 
  ggtitle("Daily new LOC dynamics (and the mean) in PostgreSQL")

res$smooth = res$added_loc
total_loc_smooth = total_loc_smooth +  
  geom_rect(inherit.aes = FALSE , data=releases, 
            aes(xmin=prestart, xmax=preend,ymin=min(res$smooth),ymax=max(res$smooth)),
            fill=alpha("red",0.6))+
  geom_rect(inherit.aes = FALSE , data=releases, 
            aes(xmin=poststart, xmax=postend,ymin=min(res$smooth),ymax=max(res$smooth)),
            fill=alpha("blue",0.5)) +
  geom_vline(xintercept=as.numeric(releases$date), linetype=4) + theme_bw()
total_loc_smooth

Cairo(width = 2600, height = 300, 
      file="figures/postgre_newloc_daily2.png", 
      type="png", pointsize=12, 
      bg = "transparent", canvas = "white", units = "px")
print(total_loc_smooth)
dev.off()


#
# CHURN
#
res <- dbGetQuery(con, 
                  paste("select sum(c.edited_lines+c.added_lines+c.removed_lines) freq, ",
                        "DATE_FORMAT(c.utc_time,'%Y-%m-%d') week from `change` c group by week",sep=""))
res$week = as.POSIXlt(res$week)

ts = zoo(res$freq, res$week)
ts_smooth <- rollmean(ts, 24, fill = list(mean(ts), NULL, mean(ts)))
res$smooth = coredata(ts_smooth)
total_churn_smooth  <- ggplot(res, aes(week, smooth)) + theme_bw() +
  geom_line() + stat_smooth() +
  scale_x_datetime("Year", breaks=as.POSIXct(paste(c(1997:2013),"-01-01",sep="")),
                   labels=c(1997:2013)) + 
  scale_y_log10("Churn, LOC") + 
  ggtitle("Weekly churn dynamics (and the smoothed curve), PostgreSQL")
total_churn_smooth 

commitfest=read.csv("../resources/commit_fest.csv",header=F)
names(commitfest) = c("name","start","end")
commitfest$start=as.POSIXlt(commitfest$start)
commitfest$end=as.POSIXlt(commitfest$end)

commit_fest_churn  <- ggplot(res, aes(week, smooth)) +  theme_bw() +
  geom_rect(inherit.aes = FALSE , data=commitfest, aes(xmin=start, xmax=as.POSIXlt(as.Date(start)+45),
                                                       ymin=min(res$smooth),ymax=max(res$smooth)),fill=alpha("red",0.4)) +
  geom_line() +
  scale_x_datetime("Year", breaks=as.POSIXct(paste(c(1997:2013),"-01-01",sep="")),
                   labels=c(1997:2013), limits=c(as.POSIXct("2009-01-01"),as.POSIXct("2014-01-01"))) + 
  scale_y_log10("Churn, LOC") + 
  ggtitle("Weekly churn smoothed curve with CommitFest events highlighted, PostgreSQL")
commit_fest_churn = commit_fest_churn  + geom_vline(xintercept=as.numeric(releases$date), linetype=4) + theme_bw()

#
# ADDED LINES
#
res <- dbGetQuery(con, 
                  paste("select sum(c.added_lines) freq, ",
                        "DATE_FORMAT(c.utc_time,'%Y-%m-%d') week from `change` c group by week",sep=""))
res$week = as.POSIXlt(res$week)
ts = zoo(res$freq, res$week)
ts_smooth <- rollmean(ts, 24, fill = list(mean(ts), NULL, mean(ts)))
res$smooth = coredata(ts_smooth)

commit_fest_added  <- ggplot(res, aes(week, smooth)) +  theme_bw() +
  geom_rect(inherit.aes = FALSE , data=commitfest, aes(xmin=start, xmax=as.POSIXlt(as.Date(start)+45),
                                                       ymin=min(res$smooth),ymax=max(res$smooth)),fill=alpha("red",0.4)) +
  geom_line() +
  scale_x_datetime("Year", breaks=as.POSIXct(paste(c(1997:2013),"-01-01",sep="")),
                   labels=c(1997:2013), limits=c(as.POSIXct("2009-01-01"),as.POSIXct("2014-01-01"))) + 
  scale_y_log10("Added LOC") + 
  ggtitle("Weekly added LOC smoothed curve with CommitFest events highlighted, PostgreSQL")
commit_fest_added = commit_fest_added + geom_vline(xintercept=as.numeric(releases$date), linetype=4) + theme_bw()


print(arrangeGrob(total_commits_smooth, commit_fest_churn, commit_fest_added), ncol=1)

ggsave(arrangeGrob(total_commits_smooth, commit_fest_churn, commit_fest_added, ncol=1), 
       file="figures/postgre_commits_dynamics.eps", width=10, height=10)

Cairo(width = 900, height = 700, 
      file="figures/postgre_commits_dynamics", 
      type="ps", pointsize=12, 
      bg = "transparent", canvas = "white", units = "px", dpi = 82)
print(arrangeGrob(total_commits_smooth, commit_fest_churn, commit_fest_added), ncol=1)
dev.off()



