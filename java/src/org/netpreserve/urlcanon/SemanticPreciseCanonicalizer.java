package org.netpreserve.urlcanon;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class SemanticPreciseCanonicalizer implements Canonicalizer {
    @Override
    public void canonicalize(ParsedUrl url) {
        WhatwgCanonicalizer.removeLeadingTrailingJunk(url);
        defaultSchemeHttp(url);
        WhatwgCanonicalizer.removeTabsAndNewlines(url);
        WhatwgCanonicalizer.lowercaseScheme(url);
        WhatwgCanonicalizer.elideDefaultPort(url);
        WhatwgCanonicalizer.cleanUpUserinfo(url);
        WhatwgCanonicalizer.twoSlashes(url);
        pctDecodeRepeatedlyExceptQuery(url);
        // TODO: reparse_host,
        WhatwgCanonicalizer.normalizeIpAddress(url);
        fixHostDots(url);
        WhatwgCanonicalizer.punycodeSpecialHost(url);
        removeUserinfo(url);
        lessDumbPctEncode(url);
        lessDumbPctRecodeQuery(url);
        WhatwgCanonicalizer.fixBackslashes(url);
        WhatwgCanonicalizer.leadingSlash(url);
        WhatwgCanonicalizer.normalizePathDots(url);
        collapseConsecutiveSlashes(url);
        WhatwgCanonicalizer.emptyPathToSlash(url);
        // TODO: alpha_reorder_query,

    }

    Pattern LEADING_OR_TRAILING_DOTS_RE = Pattern.compile("^\\.+|\\.+$");
    Pattern TWO_OR_MORE_DOTS_RE = Pattern.compile("\\.{2,}");

    private void fixHostDots(ParsedUrl url) {
        ByteString host = url.getHost();
        host = host.replaceAll(LEADING_OR_TRAILING_DOTS_RE, "");
        host = host.replaceAll(TWO_OR_MORE_DOTS_RE, ".");
        url.setHost(host);
    }

    private static final Pattern TWO_OR_MORE_SLASHES_RE = Pattern.compile("//+");

    private void collapseConsecutiveSlashes(ParsedUrl url) {
        url.setPath(url.getPath().replaceAll(TWO_OR_MORE_SLASHES_RE, "/"));
    }

    static void defaultSchemeHttp(ParsedUrl url) {
        if (url.getScheme().isEmpty()) {
            url.setScheme(new ByteString("http"));
            url.setColonAfterScheme(new ByteString(":"));
            if (!url.getPath().isEmpty()) {
                ParsedUrl.parsePathish(url, url.getPath());
            }
        }
    }

    static void pctDecodeRepeatedlyExceptQuery(ParsedUrl url) {
        url.setScheme(pctDecodeTokenRepeatedly(url.getScheme()));
        url.setUsername(pctDecodeTokenRepeatedly(url.getUsername()));
        url.setPassword(pctDecodeTokenRepeatedly(url.getPassword()));
        url.setHost(pctDecodeTokenRepeatedly(url.getHost()));
        url.setPort(pctDecodeTokenRepeatedly(url.getPort()));
        url.setPath(pctDecodeTokenRepeatedly(url.getPath()));
        url.setFragment(pctDecodeTokenRepeatedly(url.getFragment()));
    }

    static ByteString pctDecodeTokenRepeatedly(ByteString str) {
        for (;;) {
            ByteString decoded = WhatwgCanonicalizer.pctDecode(str);
            if (decoded.equals(str)) {
                return decoded;
            }
            str = decoded;
        }
    }

    static void removeUserinfo(ParsedUrl url) {
        url.setUsername(ByteString.EMPTY);
        url.setColonBeforePassword(ByteString.EMPTY);
        url.setPassword(ByteString.EMPTY);
        url.setAtSign(ByteString.EMPTY);
    }

    static final Pattern GOOGLE_PCT_ENCODE_RE = Pattern.compile("[\\x00-\\x20\\x7f-\\xff#%]");
    static final Pattern LESS_DUMB_USERINFO_ENCODE_RE = Pattern.compile("[\\x00-\\x20\\x7f-\\xff#%:@]");
    static final Pattern LESS_DUMB_PATH_ENCODE_RE = Pattern.compile("[\\x00-\\x20\\x7f-\\xff#%?]");

    static void lessDumbPctEncode(ParsedUrl url) {
        url.setScheme(WhatwgCanonicalizer.pctEncode(url.getScheme(), GOOGLE_PCT_ENCODE_RE));
        url.setScheme(WhatwgCanonicalizer.pctEncode(url.getScheme(), GOOGLE_PCT_ENCODE_RE));
        url.setUsername(WhatwgCanonicalizer.pctEncode(url.getUsername(), LESS_DUMB_USERINFO_ENCODE_RE));
        url.setPassword(WhatwgCanonicalizer.pctEncode(url.getPassword(), LESS_DUMB_USERINFO_ENCODE_RE));
        url.setHost(WhatwgCanonicalizer.pctEncode(url.getHost(), GOOGLE_PCT_ENCODE_RE));
        url.setPort(WhatwgCanonicalizer.pctEncode(url.getPort(), GOOGLE_PCT_ENCODE_RE));
        url.setPath(WhatwgCanonicalizer.pctEncode(url.getPath(), LESS_DUMB_PATH_ENCODE_RE));
        url.setFragment(WhatwgCanonicalizer.pctEncode(url.getFragment(), GOOGLE_PCT_ENCODE_RE));
    }

    static final Pattern LESS_DUMB_QUERY_ENCODE_RE = Pattern.compile("[\\x00-\\x20\\x7f-\\xff#%&=]");
    static final Pattern QUERY_PARAM_RE = Pattern.compile("([^=]*)(=[^&]*)?(&|$)");

    private static ByteString pctRecodeQueryPart(ByteString s) {
        ByteString decoded = pctDecodeTokenRepeatedly(s);
        return WhatwgCanonicalizer.pctEncode(decoded, LESS_DUMB_QUERY_ENCODE_RE);
    }

    static void lessDumbPctRecodeQuery(ParsedUrl url) {
        ByteString query = url.getQuery();
        if (query.isEmpty()) {
            return;
        }
        ByteStringBuilder out = new ByteStringBuilder(query.length());

        Matcher m = QUERY_PARAM_RE.matcher(query);
        while (m.lookingAt()) {
            ByteString key = query.subSequence(m.start(1), m.end(1));
            out.append(pctRecodeQueryPart(key));

            if (m.start(2) != -1) {
                out.append('=');
                ByteString value = query.subSequence(m.start(2) + 1, m.end(2));
                out.append(pctRecodeQueryPart(value));
            }

            if (m.start(3) < m.end()) {
                out.append('&');
            }

            if (m.end() == query.length()) {
                break;
            }
            m.region(m.end(), query.length());
        }

        url.setQuery(out.toByteString());
    }
}
