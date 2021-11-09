package net.i2p.router.networkdb.kademlia;

import net.i2p.router.Job;
import net.i2p.router.JobImpl;
import net.i2p.router.JobTiming;
import net.i2p.router.RouterContext;
import net.i2p.util.Log;

public class PrintSuccessJob extends JobImpl {
    Log _log;
    String _info;

    public PrintSuccessJob(RouterContext context, String info) {
        super(context);
        _log = getContext().logManager().getLog(getClass());
        _info = info;
    }

    @Override
    public String getName() {
        return "Print Success job";
    }

    @Override
    public void runJob() {
        _log.debug("@@YOUNG success:" + _info);
    }
}
