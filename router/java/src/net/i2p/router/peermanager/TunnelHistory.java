package net.i2p.router.peermanager;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import net.i2p.router.RouterContext;
import net.i2p.stat.RateStat;
import net.i2p.util.Log;

/**
 * Tunnel related history information
 *
 */
public class TunnelHistory {
    private RouterContext _context;
    private Log _log;
    private volatile long _lifetimeAgreedTo;
    private volatile long _lifetimeRejected;
    private volatile long _lastAgreedTo;
    private volatile long _lastRejectedCritical;
    private volatile long _lastRejectedBandwidth;
    private volatile long _lastRejectedTransient;
    private volatile long _lastRejectedProbabalistic;
    private volatile long _lifetimeFailed;
    private volatile long _lastFailed;
    private RateStat _rejectRate;
    private RateStat _failRate;
    private RateStat _processSuccessRate;
    private RateStat _processFailureRate;
    private String _statGroup;
    
    /** probabalistic tunnel rejection due to a flood of requests */
    public static final int TUNNEL_REJECT_PROBABALISTIC_REJECT = 10;
    /** tunnel rejection due to temporary cpu/job/tunnel overload */
    public static final int TUNNEL_REJECT_TRANSIENT_OVERLOAD = 20;
    /** tunnel rejection due to excess bandwidth usage */
    public static final int TUNNEL_REJECT_BANDWIDTH = 30;
    /** tunnel rejection due to system failure */
    public static final int TUNNEL_REJECT_CRIT = 50;
    
    public TunnelHistory(RouterContext context, String statGroup) {
        _context = context;
        _log = context.logManager().getLog(TunnelHistory.class);
        _statGroup = statGroup;
        createRates(statGroup);
    }
    
    private void createRates(String statGroup) {
        _rejectRate = new RateStat("tunnelHistory.rejectRate", "How often does this peer reject a tunnel request?", statGroup, new long[] { 60*1000l, 10*60*1000l, 30*60*1000l, 60*60*1000l, 24*60*60*1000l });
        _failRate = new RateStat("tunnelHistory.failRate", "How often do tunnels this peer accepts fail?", statGroup, new long[] { 60*1000l, 10*60*1000l, 30*60*1000l, 60*60*1000l, 24*60*60*1000l });
        _processSuccessRate = new RateStat("tunnelHistory.processSuccessRate", "How many messages does a tunnel process?", statGroup, new long[] { 5*60*1000l, 10*60*1000l, 30*60*1000l, 60*60*1000l, 24*60*60*1000l });
        _processFailureRate = new RateStat("tunnelHistory.processfailureRate", "How many messages does a tunnel fail?", statGroup, new long[] { 5*60*1000l, 10*60*1000l, 30*60*1000l, 60*60*1000l, 24*60*60*1000l });
        _rejectRate.setStatLog(_context.statManager().getStatLog());
        _failRate.setStatLog(_context.statManager().getStatLog());
        _processSuccessRate.setStatLog(_context.statManager().getStatLog());
        _processFailureRate.setStatLog(_context.statManager().getStatLog());
    }
    
    /** total tunnels the peer has agreed to participate in */
    public long getLifetimeAgreedTo() { return _lifetimeAgreedTo; }
    /** total tunnels the peer has refused to participate in */
    public long getLifetimeRejected() { return _lifetimeRejected; }
    /** total tunnels the peer has agreed to participate in that were later marked as failed prematurely */
    public long getLifetimeFailed() { return _lifetimeFailed; }
    /** when the peer last agreed to participate in a tunnel */
    public long getLastAgreedTo() { return _lastAgreedTo; }
    /** when the peer last refused to participate in a tunnel with level of critical */
    public long getLastRejectedCritical() { return _lastRejectedCritical; }
    /** when the peer last refused to participate in a tunnel complaining of bandwidth overload */
    public long getLastRejectedBandwidth() { return _lastRejectedBandwidth; }
    /** when the peer last refused to participate in a tunnel complaining of transient overload */
    public long getLastRejectedTransient() { return _lastRejectedTransient; }
    /** when the peer last refused to participate in a tunnel probabalistically */
    public long getLastRejectedProbabalistic() { return _lastRejectedProbabalistic; }
    /** when the last tunnel the peer participated in failed */
    public long getLastFailed() { return _lastFailed; }
    
    public void incrementProcessed(int processedSuccessfully, int failedProcessing) { 
        if (processedSuccessfully > 0)
            _processSuccessRate.addData(processedSuccessfully, 0);
        if (failedProcessing > 0)
            _processFailureRate.addData(failedProcessing, 0);
    }
    
    public void incrementAgreedTo() {
        _lifetimeAgreedTo++;
        _lastAgreedTo = _context.clock().now();
    }
    
    /**
     * @param severity how much the peer doesnt want to participate in the 
     *                 tunnel (large == more severe)
     */
    public void incrementRejected(int severity) {
        _lifetimeRejected++;
        if (severity >= TUNNEL_REJECT_CRIT) {
            _lastRejectedCritical = _context.clock().now();
            _rejectRate.addData(1, 1);
        } else if (severity >= TUNNEL_REJECT_BANDWIDTH) {
            _lastRejectedBandwidth = _context.clock().now();
            _rejectRate.addData(1, 1);
        } else if (severity >= TUNNEL_REJECT_TRANSIENT_OVERLOAD) {
            _lastRejectedTransient = _context.clock().now();
            // dont increment the reject rate in this case
        } else if (severity >= TUNNEL_REJECT_PROBABALISTIC_REJECT) {
            _lastRejectedProbabalistic = _context.clock().now();
            // dont increment the reject rate in this case
        }
    }
    public void incrementFailed() {
        _lifetimeFailed++;
        _failRate.addData(1, 1);
        _lastFailed = _context.clock().now();
    }
    
    public void setLifetimeAgreedTo(long num) { _lifetimeAgreedTo = num; }
    public void setLifetimeRejected(long num) { _lifetimeRejected = num; }
    public void setLifetimeFailed(long num) { _lifetimeFailed = num; }
    public void setLastAgreedTo(long when) { _lastAgreedTo = when; }
    public void setLastRejectedCritical(long when) { _lastRejectedCritical = when; }
    public void setLastRejectedBandwidth(long when) { _lastRejectedBandwidth = when; }
    public void setLastRejectedTransient(long when) { _lastRejectedTransient = when; }
    public void setLastRejectedProbabalistic(long when) { _lastRejectedProbabalistic = when; }
    public void setLastFailed(long when) { _lastFailed = when; }
    
    public RateStat getRejectionRate() { return _rejectRate; }
    public RateStat getFailedRate() { return _failRate; }
    public RateStat getProcessSuccessRate() { return _processSuccessRate; }
    public RateStat getProcessFailureRate() { return _processFailureRate; }
    
    public void coalesceStats() {
        if (_log.shouldLog(Log.DEBUG))
            _log.debug("Coallescing stats");
        _rejectRate.coalesceStats();
        _failRate.coalesceStats();
        _processFailureRate.coalesceStats();
        _processSuccessRate.coalesceStats();
    }
    
    private final static String NL = System.getProperty("line.separator");
    
    public void store(OutputStream out) throws IOException {
        StringBuffer buf = new StringBuffer(512);
        buf.append(NL);
        buf.append("#################").append(NL);
        buf.append("# Tunnel history").append(NL);
        buf.append("###").append(NL);
        add(buf, "lastAgreedTo", _lastAgreedTo, "When did the peer last agree to participate in a tunnel?  (milliseconds since the epoch)");
        add(buf, "lastFailed", _lastFailed, "When was the last time a tunnel that the peer agreed to participate failed?  (milliseconds since the epoch)");
        add(buf, "lastRejectedCritical", _lastRejectedCritical, "When was the last time the peer refused to participate in a tunnel?  (milliseconds since the epoch)");
        add(buf, "lastRejectedBandwidth", _lastRejectedBandwidth, "When was the last time the peer refused to participate in a tunnel?  (milliseconds since the epoch)");
        add(buf, "lastRejectedTransient", _lastRejectedTransient, "When was the last time the peer refused to participate in a tunnel?  (milliseconds since the epoch)");
        add(buf, "lastRejectedProbabalistic", _lastRejectedProbabalistic, "When was the last time the peer refused to participate in a tunnel?  (milliseconds since the epoch)");
        add(buf, "lifetimeAgreedTo", _lifetimeAgreedTo, "How many tunnels has the peer ever agreed to participate in?");
        add(buf, "lifetimeFailed", _lifetimeFailed, "How many tunnels has the peer ever agreed to participate in that failed prematurely?");
        add(buf, "lifetimeRejected", _lifetimeRejected, "How many tunnels has the peer ever refused to participate in?");
        out.write(buf.toString().getBytes());
        _rejectRate.store(out, "tunnelHistory.rejectRate");
        _failRate.store(out, "tunnelHistory.failRate");
        _processSuccessRate.store(out, "tunnelHistory.processSuccessRate");
        _processFailureRate.store(out, "tunnelHistory.processFailureRate");
    }
    
    private void add(StringBuffer buf, String name, long val, String description) {
        buf.append("# ").append(name.toUpperCase()).append(NL).append("# ").append(description).append(NL);
        buf.append("tunnels.").append(name).append('=').append(val).append(NL).append(NL);
    }
    
    public void load(Properties props) {
        _lastAgreedTo = getLong(props, "tunnels.lastAgreedTo");
        _lastFailed = getLong(props, "tunnels.lastFailed");
        _lastRejectedCritical = getLong(props, "tunnels.lastRejectedCritical");
        _lastRejectedBandwidth = getLong(props, "tunnels.lastRejectedBandwidth");
        _lastRejectedTransient = getLong(props, "tunnels.lastRejectedTransient");
        _lastRejectedProbabalistic = getLong(props, "tunnels.lastRejectedProbabalistic");
        _lifetimeAgreedTo = getLong(props, "tunnels.lifetimeAgreedTo");
        _lifetimeFailed = getLong(props, "tunnels.lifetimeFailed");
        _lifetimeRejected = getLong(props, "tunnels.lifetimeRejected");
        try {
            _rejectRate.load(props, "tunnelHistory.rejectRate", true);
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("Loading tunnelHistory.rejectRate");
            _failRate.load(props, "tunnelHistory.failRate", true);
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("Loading tunnelHistory.failRate");
            _processFailureRate.load(props, "tunnelHistory.processFailureRate", true);
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("Loading tunnelHistory.processFailureRate");
            _processSuccessRate.load(props, "tunnelHistory.processSuccessRate", true);
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("Loading tunnelHistory.processSuccessRate");
        } catch (IllegalArgumentException iae) {
            _log.warn("TunnelHistory rates are corrupt, resetting", iae);
            createRates(_statGroup);
        }
    }
    
    private final static long getLong(Properties props, String key) {
        String val = props.getProperty(key);
        if (val != null) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException nfe) {
                return 0;
            }
        }
        return 0;
    }
}
