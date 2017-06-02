/*
 * Copyright (c) 2016 OpenSilk Productions LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.upnp.cds.featurelist;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.StreamResponseMessage;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.control.IncomingActionResponseMessage;
import org.fourthline.cling.transport.impl.SOAPActionProcessorImpl;
import org.fourthline.cling.transport.spi.SOAPActionProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Created by drew on 12/20/16.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class FeatureListTests {

    @Test
    public void test_parse() throws Exception {
        ActionInvocation ai = makeActionInvocation();
        //pull the features
        Features features = new FeaturesParser().parse(ai.getOutput("FeatureList").toString());
        assertThat(features.getFeatures().size()).isEqualTo(1);
        Feature feature = features.getFeatures().get(0);
        assertThat(feature).isInstanceOf(BasicView.class);
        assertThat(feature.getName()).isEqualTo(BasicView.NAME);
        assertThat(feature.getVersion()).isEqualTo(1);
        BasicView basicView = (BasicView) feature;
        assertThat(basicView.getAudioItemId()).isEqualTo("1");
        assertThat(basicView.getVideoItemId()).isEqualTo("2");
    }

    @Test
    public void test_callback() throws Exception {
        ActionInvocation ai = makeActionInvocation();
        final AtomicBoolean called = new AtomicBoolean(false);
        XGetFeatureListCallback fc = new XGetFeatureListCallback(null) {
            @Override
            public void received(ActionInvocation actionInvocation, Features features) {
                called.set(true);
                assertThat(features.getFeatures().size()).isEqualTo(1);
                Feature f = features.getFeatures().get(0);
                assertThat(f).isOfAnyClassIn(BasicView.class);
                BasicView b = (BasicView) f;
                assertThat(b.getContainers().size()).isEqualTo(3);
                assertThat(b.getAudioItemId()).isEqualTo("1");
                assertThat(b.getVideoItemId()).isEqualTo("2");
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {

            }
        };
        fc.success(ai);
        assertThat(called.get()).isEqualTo(true);
    }


    ActionInvocation makeActionInvocation() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("getfeaturelistresponse.xml");
        assertThat(is).isNotNull();
        String xml = IOUtils.toString(is, "UTF-8");
        is.close();
        assertThat(xml).isNotEmpty();
        //Minimal stuffs needed to convert the xml into an action invocation output
        StreamResponseMessage srm = new StreamResponseMessage(xml);
        IncomingActionResponseMessage iarm = new IncomingActionResponseMessage(srm);
        SOAPActionProcessor sap = new SOAPActionProcessorImpl();
        ActionInvocation ai = new ActionInvocation(new XGetFeatureListAction());
        sap.readBody(iarm, ai);
        return ai;
    }
}
