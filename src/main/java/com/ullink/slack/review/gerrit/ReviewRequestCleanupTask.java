package com.ullink.slack.review.gerrit;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jobs.DeleteMessageJob;
import jobs.RefreshMessageJob;
import com.ullink.slack.review.HttpHelper;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequestService;
import com.ullink.slack.simpleslackapi.SlackSession;

public class ReviewRequestCleanupTask implements Runnable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewRequestCleanupTask.class);

    private ReviewRequestService    reviewRequestService;
    private GerritChangeInfoService gerritChangeInfoService;
    private SlackSession            session;
    private ExecutorService executorService;
    private ChangeInfoFormatter changeInfoDecorator;

    public ReviewRequestCleanupTask(ReviewRequestService reviewRequestService, GerritChangeInfoService gerritChangeInfoService, ChangeInfoFormatter changeInfoDecorator, SlackSession session, ExecutorService executorService)
    {
        this.session = session;
        this.reviewRequestService = reviewRequestService;
        this.gerritChangeInfoService = gerritChangeInfoService;
        this.executorService = executorService;
        this.changeInfoDecorator = changeInfoDecorator;
    }

    @Override
    public void run()
    {
        Collection<String> pendingChanges = reviewRequestService.getAllRequestedReviews();
        for (String changeId : pendingChanges)
        {
            try
            {
                if (gerritChangeInfoService.isMergedOrAbandoned(changeId))
                {
                    executorService.submit(new DeleteMessageJob(changeId, session, reviewRequestService));
                }
                else
                {
                    executorService.submit(new RefreshMessageJob(changeId, session, reviewRequestService, gerritChangeInfoService, changeInfoDecorator));
                }
            }
            catch (Throwable t)
            {
                LOGGER.error("Unexpected error, t");
            }
        }
    }

}
