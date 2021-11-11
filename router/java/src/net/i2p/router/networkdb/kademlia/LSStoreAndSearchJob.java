package net.i2p.router.networkdb.kademlia;

import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.data.LeaseSet;
import net.i2p.data.i2np.DatabaseStoreMessage;
import net.i2p.data.router.RouterInfo;
import net.i2p.router.*;
import net.i2p.router.client.ClientConnectionRunner;
import net.i2p.router.client.ClientManagerFacadeImpl;
import net.i2p.router.message.SendMessageDirectJob;
import net.i2p.util.Log;

import static net.i2p.router.utils.aof;
import static net.i2p.router.utils.loadRIFromFile;

public class LSStoreAndSearchJob extends JobImpl {
    Log _log;
    FloodfillNetworkDatabaseFacade _facade;
    RouterContext _context;

    public LSStoreAndSearchJob(RouterContext context, FloodfillNetworkDatabaseFacade facade) {
        super(context);
        _log = getContext().logManager().getLog(getClass());
        _facade = facade;
        _context = context;
    }

    @Override
    public String getName() {
        return "LS Store And SearchJob";
    }

    @Override
    public void runJob() {
        // before we visit the actual target, store the ls directly to target RI
        // and then wo query it for the stored ls
        ClientManagerFacadeImpl managerFacade = (ClientManagerFacadeImpl) _context.clientManager();
        Destination self = utils.getDestination(_context.getProperty("custom.selfHS"));
        ClientConnectionRunner runner = managerFacade.getManager().getRunners().get(self);
        if (runner == null) {
            _log.debug("@@YOUNG runner not ready, waiting...");
        } else {
            LeaseSet ls2 = runner.getLeaseSet(self.getHash());
            if (ls2.verifySignature()) {
                _log.debug("@@YOUNG ls2 verify SUCCESS");
            } else {
                _log.debug("@@YOUNG ls2 verify FAIL");
            }
            DatabaseStoreMessage lsDSM = new DatabaseStoreMessage(_context);
            lsDSM.setEntry(ls2);
            lsDSM.setMessageExpiration(_context.clock().now() + 3 * 60 * 1000);
            int timeout = 10 * 10 * 1000;
            RouterInfo ri = loadRIFromFile(_context.getProperty("custom.queryTarget"));
            assert ri != null;
            SendMessageDirectJob j = new SendMessageDirectJob(_context, lsDSM, ri.getHash(),
                null, null, null, timeout, OutNetMessage.PRIORITY_EXPLORATORY);
            j.runJob();
            _log.debug("LSS: fire off DSM:" + lsDSM);
            aof("C:\\Users\\DD12\\AppData\\Local\\I2P\\logs\\extra.txt", utils.getFormatTime() + " jobID:" + getJobId() + " LSS: fire off DSM:" + lsDSM);
            try {
                // sleep 30s before search it...
                Thread.sleep(60 * 1000);
            } catch (Exception e) {
                _log.error("LLS ERROR:" + e);
            }
            // query it later
            Hash queried = ls2.getHash();
            Job success = new PrintSuccessJob(_context, "Query hash:" + queried);
            Job failure = new PrintFailureJob(_context, "Query hash:" + queried);
            SearchJob sj = new SearchJob(_context, _facade, queried, success, failure, 12 * 60 * 1000, false, true);
            aof("C:\\Users\\DD12\\AppData\\Local\\I2P\\logs\\extra.txt", utils.getFormatTime() + " jobID:" + sj.getJobId() + " :YOUNG sendLeaseSearch to " + ri.getIdentity().getHash()
                + " for " + queried);
            sj.sendLeaseSearch2(ri);
            _log.debug("LSS: fire off DLM");
        }
        int minutes = 2;
        requeue(minutes * 60 * 1000);
        _log.debug(getName() + " will be rerun at " + minutes + " minutes later");
    }
}
