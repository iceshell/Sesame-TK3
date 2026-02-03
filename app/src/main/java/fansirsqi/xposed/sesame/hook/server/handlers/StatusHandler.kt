package fansirsqi.xposed.sesame.hook.server.handlers

import fansirsqi.xposed.sesame.hook.ModuleStatusReporter
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Response

class StatusHandler(secretToken: String) : BaseHandler(secretToken) {

    override fun onGet(session: IHTTPSession): Response {
        return ok(ModuleStatusReporter.getStatusSnapshot("http_get"))
    }

    override fun onPost(session: IHTTPSession, body: String?): Response {
        return methodNotAllowed()
    }
}
