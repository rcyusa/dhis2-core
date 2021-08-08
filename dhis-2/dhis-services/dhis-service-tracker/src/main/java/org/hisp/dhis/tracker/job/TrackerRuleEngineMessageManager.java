/*
 * Copyright (c) 2004-2021, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.tracker.job;

import java.io.IOException;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.hisp.dhis.artemis.MessageManager;
import org.hisp.dhis.artemis.Topics;
import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Producer and consumer for handling program rule actions.
 *
 * @author Zubair Asghar
 */
@Component
public class TrackerRuleEngineMessageManager extends BaseMessageManager
{
    private final ObjectFactory<TrackerRuleEngineThread> trackerRuleEngineThreadObjectFactory;

    public TrackerRuleEngineMessageManager(
        MessageManager messageManager,
        AsyncTaskExecutor taskExecutor,
        RenderService renderService,
        ObjectFactory<TrackerRuleEngineThread> trackerRuleEngineThreadObjectFactory )
    {
        super( messageManager, taskExecutor, renderService );
        this.trackerRuleEngineThreadObjectFactory = trackerRuleEngineThreadObjectFactory;
    }

    @Override
    public String getTopic()
    {
        return Topics.TRACKER_IMPORT_RULE_ENGINE_TOPIC_NAME;
    }

    @JmsListener( destination = Topics.TRACKER_IMPORT_RULE_ENGINE_TOPIC_NAME, containerFactory = "jmsQueueListenerContainerFactory" )
    public void consume( TextMessage message )
        throws JMSException,
        IOException
    {
        TrackerSideEffectDataBundle bundle = toBundle( message );

        if ( bundle == null )
        {
            return;
        }

        JobConfiguration jobConfiguration = new JobConfiguration( "", JobType.TRACKER_IMPORT_RULE_ENGINE_JOB,
            bundle.getAccessedBy(), true );

        bundle.setJobConfiguration( jobConfiguration );

        TrackerRuleEngineThread notificationThread = trackerRuleEngineThreadObjectFactory.getObject();

        notificationThread.setSideEffectDataBundle( bundle );

        executeJob( notificationThread );
    }
}