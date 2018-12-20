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
package com.meituan.dorado.common.exception;

/**
 * 业务异常，服务端业务接口方法执行异常时抛出
 */
public class ApplicationException extends DoradoException {

    public ApplicationException(String msg) {
        super(msg);
    }

    public ApplicationException(Throwable cause) {
        super(cause);
    }

    public ApplicationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
