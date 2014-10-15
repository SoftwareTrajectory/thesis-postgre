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
#
# connect to DB
# 
con <- dbConnect(MySQL(), user="postgre", password="postgre", dbname="postgre2", host="localhost")
#
# qery DB function
#
commits_summary = function(start,end){
  res <- dbGetQuery(con, 
    paste("select count(distinct(c.commit_hash)) freq, ",
    "WEEK(c.utc_time,1) week from `change` c where ",
    "c.utc_time between \"",start,"\" AND \"", end,"\" group by week",sep=""))
  res
}
#
# get dataframe of weekly commits
#
dat=data.frame(week=c(1:53))
for(interval in c(1996:2013)){
  yearly_commits = commits_summary(paste(interval,"-01-01",sep=""),
                                   paste(interval,"-12-30",sep=""))
  tmp = data.frame(week=c(1:53))
  tmp = merge(tmp,yearly_commits,all.x=T)
  dat = cbind(dat,tmp$freq)
}
#
# massage it
#
names(dat) = c("week",paste(c(1996:2013)))
dm = melt(dat,id.var="week")
#
# plot weekly averages
#
p_commits <- ggplot(dm, aes(factor(week), value)) + geom_boxplot() +
  scale_x_discrete("Week of the year") + scale_y_continuous("Commits") + 
  ggtitle("Weekly commits variance, PostgreSQL") + theme_bw()
p_commits

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
  ggtitle("Weekly commits dynamics (and the smoothed curve) in PostgreSQL")
total_commits_smooth 

grid.arrange(total_commits_smooth, p_commits)

#
# qery DB function
#
commits_summary = function(start,end){
  res <- dbGetQuery(con, 
        paste("select sum(c.edited_lines+c.added_lines+c.removed_lines) freq, ",
        "WEEK(c.utc_time,1) week from `change` c where ",
        "c.utc_time between \"",start,"\" AND \"", end,"\" group by week",sep=""))
  res
}
#
# get dataframe of weekly commits
#
dat=data.frame(week=c(1:53))
for(interval in c(1996:2013)){
  yearly_commits = commits_summary(paste(interval,"-01-01",sep=""),
                                   paste(interval,"-12-30",sep=""))
  tmp = data.frame(week=c(1:53))
  tmp = merge(tmp,yearly_commits,all.x=T)
  dat = cbind(dat,tmp$freq)
}
#
# massage it
#
names(dat) = c("week",paste(c(1996:2013)))
dm = melt(dat,id.var="week")
#
# plot weekly averages
#
p_weekly_churns <- ggplot(dm, aes(factor(week), value)) + geom_boxplot() + theme_bw() +
  scale_x_discrete("Week of the year") + scale_y_log10("Churn, LOC") + 
  ggtitle("Weekly churn variance, PostgreSQL")
p_weekly_churns

res <- dbGetQuery(con, 
    paste("select sum(c.edited_lines+c.added_lines) freq, ",
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
      ymin=min(res$smooth),ymax=max(res$smooth)),fill=alpha("red",0.6)) +
  geom_line() +
  scale_x_datetime("Year", breaks=as.POSIXct(paste(c(1997:2013),"-01-01",sep="")),
                   labels=c(1997:2013), limits=c(as.POSIXct("2009-01-01"),as.POSIXct("2014-01-01"))) + 
  scale_y_log10("Churn, LOC") + 
  ggtitle("Weekly churn smoothed curve with CommitFest events, PostgreSQL")
commit_fest_churn


ggsave(arrangeGrob(total_commits_smooth, p_commits, ncol=1), file="figures/postgre_commits_dynamics.eps", 
       width=10, height=6)
