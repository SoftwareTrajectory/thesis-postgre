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
library(zoo)

releases=read.table("../resources/releases.csv",as.is=T,sep=",")
names(releases)=c("id","date")
str(releases)
releases$date=as.POSIXct(releases$date)

#
# connect to DB
# 
con <- dbConnect(MySQL(), user="postgre", password="postgre", dbname="postgre2", host="localhost")
#
# COMMITS HISTORY, qery DB function
#
res <- dbGetQuery(con, 
      paste("select count(distinct(c.commit_hash)) freq, ",
      "DATE_FORMAT(c.utc_time,'%Y-%m-%d') week from `change` c group by week",sep=""))
res$week = as.POSIXlt(res$week)

ts = zoo(res$freq, res$week)
ts_smooth <- rollmean(ts, 24, fill = list(mean(ts), NULL, mean(ts)))
res$smooth = coredata(ts_smooth)

total_commits_smooth <- ggplot(res, aes(week, smooth)) + geom_line() + theme_bw() +
  scale_x_datetime("Year", breaks=as.POSIXct(paste(c(1997:2013),"-01-01",sep="")),
  labels=c(1997:2013)) + stat_smooth() +
  scale_y_continuous("Commits") + 
  ggtitle("Weekly commits dynamics (and the mean) in PostgreSQL")
total_commits_smooth = total_commits_smooth + geom_vline(xintercept=as.numeric(releases$date), linetype=4) + theme_bw()

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
total_churn_smooth=  total_churn_smooth+ geom_vline(xintercept=as.numeric(releases$date), linetype=4) + theme_bw()

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
