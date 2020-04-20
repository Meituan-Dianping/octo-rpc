/*
 * Copyright 2018 Meituan Dianping. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meituan.dorado.transport.http;

import com.meituan.dorado.rpc.handler.http.DefaultHttpResponse;

public interface HttpSender {

    void send(DefaultHttpResponse httpResponse);

    void sendObjectJson(Object object);

    void sendErrorResponse(String errorMsg);

    class ReturnMessage {
        private Boolean success;
        private String result;

        public ReturnMessage(Boolean success, String result) {
            this.success = success;
            this.result = result;
        }

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            success = success;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }
}
