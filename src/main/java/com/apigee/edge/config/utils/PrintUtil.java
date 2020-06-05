/**
 * Copyright (C) 2016 Apigee Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apigee.edge.config.utils;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.Set;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;


public class PrintUtil {

    public static String formatRequest(HttpRequest request) {

        String prettyRequest = "\n\n\nRequest prepared for the server \n **************************\n";

        // Print all headers except auth

        prettyRequest = prettyRequest + request.getRequestMethod() + "  " + request.getUrl();

        HttpHeaders headers = request.getHeaders();

        Set<String> tempheadersmap = headers.keySet();

        for (Iterator<String> iter = tempheadersmap.iterator(); iter.hasNext(); ) {

            try {
                String headerkey = iter.next();
                if (!headerkey.trim().equalsIgnoreCase("Authorization")) {
                    String headervalue = ""+headers.get(headerkey);
                    prettyRequest = prettyRequest + "\n" + headerkey + ": " + headervalue;
                }else {
                    String headervalue = ""+headers.get(headerkey);
                    String arr[] = headervalue.split(" ", 2);
                    String prefix = arr[0];   // Basic, Bearer
                    prettyRequest = prettyRequest + "\n" + 
                                    "authorization" + ": " + prefix + " [Not shown in log]";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        try {
        if (request.getRequestMethod().compareTo(HttpMethods.POST) == 0  ){

            if (request.getContent()!=null && request.getContent().getType() !=null)
            {
                prettyRequest = prettyRequest + "\n" + "content-type" + ": " + request.getContent().getType();

                //https://github.com/apigee/apigee-config-maven-plugin/issues/63 Not print KVM Request as it could have sensitive data
                if (request.getUrl().toString().contains("/keyvaluemaps") || request.getUrl().toString().contains("/aliases")) {
                    prettyRequest = prettyRequest + "\n [Request body may contain sensitive data, not shown] \n";
                }
                else if (!request.getContent().getType().contains("octet"))
                {
	                ByteArrayOutputStream out = new ByteArrayOutputStream();
	                request.getContent().writeTo(out);
	                prettyRequest = prettyRequest + "\n [Request body]\n" + out.toString();
                } 
                else {
                    prettyRequest = prettyRequest + "\n [Request body contains data, not shown] \n";
                }
            }

        }
        }catch (Exception e){
            e.printStackTrace();
        }

        return  prettyRequest;
    }


    public static String formatResponse(HttpResponse response, String body) {

        String prettyString = "\n\n\nResponse returned by the server \n **************************\n";

        // Print all headers except auth

        prettyString = prettyString + response.getStatusCode() + "  " + response.getStatusMessage();

        HttpHeaders headers = response.getHeaders();

        Set<String> tempheadersmap = headers.keySet();

        for (Iterator<String> iter = tempheadersmap.iterator(); iter.hasNext(); ) {

            try {
                String headerkey = iter.next();
                if (!headerkey.trim().equalsIgnoreCase("Authorization")) {
                    String headervalue = ""+headers.get(headerkey);
                    prettyString = prettyString + "\n" + headerkey + ": " + headervalue;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        // print response body
        prettyString = prettyString + "\n" + body;

        return  prettyString;
    }
}
