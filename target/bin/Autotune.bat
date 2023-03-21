@REM ----------------------------------------------------------------------------
@REM Copyright 2001-2004 The Apache Software Foundation.
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM      http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM ----------------------------------------------------------------------------
@REM

@echo off

set ERROR_CODE=0

:init
@REM Decide how to startup depending on the version of windows

@REM -- Win98ME
if NOT "%OS%"=="Windows_NT" goto Win9xArg

@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal

@REM -- 4NT shell
if "%eval[2+2]" == "4" goto 4NTArgs

@REM -- Regular WinNT shell
set CMD_LINE_ARGS=%*
goto WinNTGetScriptDir

@REM The 4NT Shell from jp software
:4NTArgs
set CMD_LINE_ARGS=%$
goto WinNTGetScriptDir

:Win9xArg
@REM Slurp the command line arguments.  This loop allows for an unlimited number
@REM of arguments (up to the command line limit, anyway).
set CMD_LINE_ARGS=
:Win9xApp
if %1a==a goto Win9xGetScriptDir
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto Win9xApp

:Win9xGetScriptDir
set SAVEDIR=%CD%
%0\
cd %0\..\.. 
set BASEDIR=%CD%
cd %SAVEDIR%
set SAVE_DIR=
goto repoSetup

:WinNTGetScriptDir
set BASEDIR=%~dp0\..

:repoSetup


if "%JAVACMD%"=="" set JAVACMD=java

if "%REPO%"=="" set REPO=%BASEDIR%\repo

set CLASSPATH="%BASEDIR%"\etc;"%REPO%"\com\pubnub\pubnub-gson\5.2.1\pubnub-gson-5.2.1.jar;"%REPO%"\com\squareup\okhttp3\logging-interceptor\3.12.6\logging-interceptor-3.12.6.jar;"%REPO%"\com\squareup\retrofit2\retrofit\2.6.2\retrofit-2.6.2.jar;"%REPO%"\org\slf4j\slf4j-api\1.7.28\slf4j-api-1.7.28.jar;"%REPO%"\com\squareup\retrofit2\converter-gson\2.6.2\converter-gson-2.6.2.jar;"%REPO%"\com\fasterxml\jackson\core\jackson-databind\2.12.3\jackson-databind-2.12.3.jar;"%REPO%"\com\fasterxml\jackson\core\jackson-annotations\2.12.3\jackson-annotations-2.12.3.jar;"%REPO%"\com\fasterxml\jackson\dataformat\jackson-dataformat-cbor\2.12.3\jackson-dataformat-cbor-2.12.3.jar;"%REPO%"\org\jetbrains\annotations\17.0.0\annotations-17.0.0.jar;"%REPO%"\com\fasterxml\jackson\core\jackson-core\2.12.4\jackson-core-2.12.4.jar;"%REPO%"\io\fabric8\kubernetes-client\4.13.0\kubernetes-client-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-core\4.13.0\kubernetes-model-core-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-common\4.13.0\kubernetes-model-common-4.13.0.jar;"%REPO%"\com\fasterxml\jackson\module\jackson-module-jaxb-annotations\2.11.2\jackson-module-jaxb-annotations-2.11.2.jar;"%REPO%"\jakarta\xml\bind\jakarta.xml.bind-api\2.3.2\jakarta.xml.bind-api-2.3.2.jar;"%REPO%"\jakarta\activation\jakarta.activation-api\1.2.1\jakarta.activation-api-1.2.1.jar;"%REPO%"\javax\annotation\javax.annotation-api\1.3.2\javax.annotation-api-1.3.2.jar;"%REPO%"\javax\xml\bind\jaxb-api\2.3.0\jaxb-api-2.3.0.jar;"%REPO%"\io\fabric8\kubernetes-model-rbac\4.13.0\kubernetes-model-rbac-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-admissionregistration\4.13.0\kubernetes-model-admissionregistration-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-apps\4.13.0\kubernetes-model-apps-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-autoscaling\4.13.0\kubernetes-model-autoscaling-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-apiextensions\4.13.0\kubernetes-model-apiextensions-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-batch\4.13.0\kubernetes-model-batch-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-certificates\4.13.0\kubernetes-model-certificates-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-coordination\4.13.0\kubernetes-model-coordination-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-discovery\4.13.0\kubernetes-model-discovery-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-events\4.13.0\kubernetes-model-events-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-extensions\4.13.0\kubernetes-model-extensions-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-networking\4.13.0\kubernetes-model-networking-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-metrics\4.13.0\kubernetes-model-metrics-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-policy\4.13.0\kubernetes-model-policy-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-scheduling\4.13.0\kubernetes-model-scheduling-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-settings\4.13.0\kubernetes-model-settings-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-storageclass\4.13.0\kubernetes-model-storageclass-4.13.0.jar;"%REPO%"\io\fabric8\kubernetes-model-node\4.13.0\kubernetes-model-node-4.13.0.jar;"%REPO%"\com\squareup\okhttp3\okhttp\3.12.12\okhttp-3.12.12.jar;"%REPO%"\com\squareup\okio\okio\1.15.0\okio-1.15.0.jar;"%REPO%"\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.11.2\jackson-dataformat-yaml-2.11.2.jar;"%REPO%"\org\yaml\snakeyaml\1.26\snakeyaml-1.26.jar;"%REPO%"\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.11.2\jackson-datatype-jsr310-2.11.2.jar;"%REPO%"\io\fabric8\zjsonpatch\0.3.0\zjsonpatch-0.3.0.jar;"%REPO%"\com\github\mifmif\generex\1.0.2\generex-1.0.2.jar;"%REPO%"\dk\brics\automaton\automaton\1.11-8\automaton-1.11-8.jar;"%REPO%"\org\json\json\20201115\json-20201115.jar;"%REPO%"\org\eclipse\jetty\jetty-server\9.4.44.v20210927\jetty-server-9.4.44.v20210927.jar;"%REPO%"\javax\servlet\javax.servlet-api\3.1.0\javax.servlet-api-3.1.0.jar;"%REPO%"\org\eclipse\jetty\jetty-http\9.4.44.v20210927\jetty-http-9.4.44.v20210927.jar;"%REPO%"\org\eclipse\jetty\jetty-io\9.4.44.v20210927\jetty-io-9.4.44.v20210927.jar;"%REPO%"\org\eclipse\jetty\jetty-servlets\9.4.44.v20210927\jetty-servlets-9.4.44.v20210927.jar;"%REPO%"\org\eclipse\jetty\jetty-continuation\9.4.44.v20210927\jetty-continuation-9.4.44.v20210927.jar;"%REPO%"\org\eclipse\jetty\jetty-util\9.4.44.v20210927\jetty-util-9.4.44.v20210927.jar;"%REPO%"\org\eclipse\jetty\jetty-servlet\9.4.44.v20210927\jetty-servlet-9.4.44.v20210927.jar;"%REPO%"\org\eclipse\jetty\jetty-security\9.4.44.v20210927\jetty-security-9.4.44.v20210927.jar;"%REPO%"\org\eclipse\jetty\jetty-util-ajax\9.4.44.v20210927\jetty-util-ajax-9.4.44.v20210927.jar;"%REPO%"\org\apache\logging\log4j\log4j-slf4j-impl\2.17.1\log4j-slf4j-impl-2.17.1.jar;"%REPO%"\org\apache\logging\log4j\log4j-api\2.17.1\log4j-api-2.17.1.jar;"%REPO%"\org\apache\logging\log4j\log4j-core\2.17.1\log4j-core-2.17.1.jar;"%REPO%"\com\udojava\EvalEx\2.7\EvalEx-2.7.jar;"%REPO%"\io\prometheus\simpleclient\0.14.1\simpleclient-0.14.1.jar;"%REPO%"\io\prometheus\simpleclient_tracer_otel\0.14.1\simpleclient_tracer_otel-0.14.1.jar;"%REPO%"\io\prometheus\simpleclient_tracer_common\0.14.1\simpleclient_tracer_common-0.14.1.jar;"%REPO%"\io\prometheus\simpleclient_tracer_otel_agent\0.14.1\simpleclient_tracer_otel_agent-0.14.1.jar;"%REPO%"\io\prometheus\simpleclient_hotspot\0.14.1\simpleclient_hotspot-0.14.1.jar;"%REPO%"\io\prometheus\simpleclient_httpserver\0.14.1\simpleclient_httpserver-0.14.1.jar;"%REPO%"\io\prometheus\simpleclient_common\0.14.1\simpleclient_common-0.14.1.jar;"%REPO%"\io\prometheus\simpleclient_servlet\0.14.1\simpleclient_servlet-0.14.1.jar;"%REPO%"\io\prometheus\simpleclient_servlet_common\0.14.1\simpleclient_servlet_common-0.14.1.jar;"%REPO%"\org\apache\httpcomponents\httpclient\4.5.13\httpclient-4.5.13.jar;"%REPO%"\org\apache\httpcomponents\httpcore\4.4.13\httpcore-4.4.13.jar;"%REPO%"\commons-logging\commons-logging\1.2\commons-logging-1.2.jar;"%REPO%"\commons-codec\commons-codec\1.11\commons-codec-1.11.jar;"%REPO%"\com\google\code\gson\gson\2.9.0\gson-2.9.0.jar;"%REPO%"\org\autotune\autotune\0.0.8_mvp\autotune-0.0.8_mvp.jar
set EXTRA_JVM_ARGUMENTS=
goto endInit

@REM Reaching here means variables are defined and arguments have been captured
:endInit

%JAVACMD% %JAVA_OPTS% %EXTRA_JVM_ARGUMENTS% -classpath %CLASSPATH_PREFIX%;%CLASSPATH% -Dapp.name="Autotune" -Dapp.repo="%REPO%" -Dbasedir="%BASEDIR%" com.autotune.Autotune %CMD_LINE_ARGS%
if ERRORLEVEL 1 goto error
goto end

:error
if "%OS%"=="Windows_NT" @endlocal
set ERROR_CODE=1

:end
@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" goto endNT

@REM For old DOS remove the set variables from ENV - we assume they were not set
@REM before we started - at least we don't leave any baggage around
set CMD_LINE_ARGS=
goto postExec

:endNT
@endlocal

:postExec

if "%FORCE_EXIT_ON_ERROR%" == "on" (
  if %ERROR_CODE% NEQ 0 exit %ERROR_CODE%
)

exit /B %ERROR_CODE%
