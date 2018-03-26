package eu.faircode.netguard;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import cn.banny.utils.StringUtils;

/**
 * tcp port redirect rule
 * Created by zhkl0228 on 2018/1/9.
 */

class TcpRedirectRule {

    private static final String TAG = TcpRedirectRule.class.getSimpleName();

    private final Pattern socketPattern;
    private final String redirectAddr;
    private final int redirectPort;

    private TcpRedirectRule(Pattern socketPattern, String redirectAddr, int redirectPort) {
        this.socketPattern = socketPattern;
        this.redirectAddr = redirectAddr;
        this.redirectPort = redirectPort;
    }

    Allowed createRedirect(Packet packet) {
        if (socketPattern.matcher(packet.daddr + ':' + packet.dport).matches()) {
            Log.d(TAG, "Redirect " + packet.daddr + ':' + packet.dport + " to " + redirectAddr + ':' + redirectPort);
            return new Allowed(redirectAddr, redirectPort);
        }
        return null;
    }

    // *:443->localhost:8888 pattern
    private static final Pattern RULE_PATTERN = Pattern.compile("([\\*\\d\\.]+):(\\d+)->([\\*\\w\\.]+):(\\d+)");

    /**
     * parse tcp redirect rules
     * @param rules example: *:443->localhost:8888,*:8443->localhost:8888,120.35.*.*:8643->localhost:8888
     */
    static TcpRedirectRule[] parseTcpRedirectRules(String rules) {
        if (StringUtils.isEmpty(rules)) {
            return new TcpRedirectRule[0];
        }
        List<TcpRedirectRule> list = new ArrayList<>(10);
        Matcher matcher = RULE_PATTERN.matcher(rules);
        while (matcher.find()) {
            String pattern = matcher.group(1).replace("*", "[\\d\\.]+") + ':' + matcher.group(2);
            String raddr = matcher.group(3);
            int rport = Integer.parseInt(matcher.group(4));
            try {
                list.add(new TcpRedirectRule(Pattern.compile(pattern), raddr, rport));
                Log.d(TAG, "Add tcp redirect rule: " + pattern + "->" + raddr + ':' + rport);
            } catch (PatternSyntaxException e) {
                Log.w(TAG, "compile rule pattern failed: " + pattern, e);
            }
        }
        return list.toArray(new TcpRedirectRule[0]);
    }
}
