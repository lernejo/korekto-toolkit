package com.github.lernejo.korekto.toolkit.thirdparty.github

import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.SecureDigestAlgorithm
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.eclipse.jgit.api.GitCommand
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.kohsuke.github.GitHubBuilder
import org.testcontainers.shaded.org.bouncycastle.openssl.PEMKeyPair
import org.testcontainers.shaded.org.bouncycastle.openssl.PEMParser
import org.testcontainers.shaded.org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.File
import java.io.FileReader
import java.net.HttpURLConnection
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.time.Instant
import java.util.*

object GitHubAuthenticationHolder {
    val auth: GitHubAuthentication by lazy {
        val token = System.getProperty("github_token")
        val appId = System.getProperty("github_app_id")
        val appPk = System.getProperty("github_app_pk")
        if (token != null) {
            TokenGitHubAuthentication(token)
        } else if (appId != null && appPk != null) {
            AppGitHubAuthentication(appId, appPk)
        } else {
            NoopTokenGitHubAuthentication()
        }
    }
}

interface GitHubAuthentication {
    val type: String
    fun <R, C : GitCommand<R>> configure(command: TransportCommand<in C, R>)
    fun configure(conn: HttpURLConnection)
    fun configure(builder: GitHubBuilder)
}

class NoopTokenGitHubAuthentication : GitHubAuthentication {
    override val type = "noop"

    override fun <R, C : GitCommand<R>> configure(command: TransportCommand<in C, R>) {
    }

    override fun configure(conn: HttpURLConnection) {
    }

    override fun configure(builder: GitHubBuilder) {
    }
}

data class TokenGitHubAuthentication(val token: String) : GitHubAuthentication {
    override val type = "token"

    override fun <R, C : GitCommand<R>> configure(command: TransportCommand<in C, R>) {
        command
            .setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
    }

    override fun configure(conn: HttpURLConnection) {
        conn.setRequestProperty("Authorization", "token $token")
    }

    override fun configure(builder: GitHubBuilder) {
        builder.withOAuthToken(token)
    }
}

class AppGitHubAuthentication(private val appId: String, appPk: String) : GitHubAuthentication {
    companion object {
        init {
            Security.removeProvider("BC") //remove old/legacy Android-provided BC provider
            Security.addProvider(BouncyCastleProvider()) // add 'real'/correct BC provider
        }
    }

    private val privateKey = readPrivateKey(appPk)
    private var jwt: Jwt? = null
    private val tokensByUser: MutableMap<String, InstallationToken> = mutableMapOf()

    override val type = "app-$appId (installation: " + getToken().installationId + ")"

    override fun <R, C : GitCommand<R>> configure(command: TransportCommand<in C, R>) {
        command.setCredentialsProvider(UsernamePasswordCredentialsProvider("x-access-token", getToken().token))
    }

    override fun configure(conn: HttpURLConnection) {
        val token = getToken().token
        conn.setRequestProperty("Authorization", "Bearer $token")
    }

    override fun configure(builder: GitHubBuilder) {
        builder.withAppInstallationToken(getToken().token)
    }

    private fun getToken(): InstallationToken {
        val user = System.getProperty("github_user") ?: throw IllegalStateException("Missing github_user env prop")
        val token = tokensByUser[user]
        if (token == null || token.isExpired()) {
            tokensByUser[user] = refreshToken(user)
        }
        return tokensByUser[user]!!
    }

    private fun refreshToken(user: String): InstallationToken {
        val jwt = getJwt()
        val gitHubApp = GitHubBuilder().withJwtToken(jwt).build()
        val installation = gitHubApp.app.getInstallationByUser(user)

        val tokenResponse = installation.createToken().create()

        return InstallationToken(tokenResponse.token, installation.id, tokenResponse.expiresAt.toInstant())
    }

    private fun getJwt(): String {
        if (jwt == null || jwt!!.isExpired()) {
            jwt = createJWT(appId, 590000, privateKey)
        }
        return jwt!!.token
    }
}

class Jwt(val token: String, private val start: Long, private val duration: Long) {
    fun isExpired() = (start + duration - 10_000) > System.currentTimeMillis()
}

class InstallationToken(val token: String, val installationId: Long, private val expiresAt: Instant) {
    fun isExpired() = expiresAt.isAfter(Instant.now().minusSeconds(60L))
}

fun readPrivateKey(filename: String): PrivateKey {
    val pemParser = PEMParser(FileReader(File(filename)))
    val o: PEMKeyPair = pemParser.readObject() as PEMKeyPair
    val converter = JcaPEMKeyConverter().setProvider("BC")
    val kp = converter.getKeyPair(o)
    return kp.private
}

fun createJWT(githubAppId: String, ttlMillis: Long, privateKey: PrivateKey): Jwt {
    //The JWT signature algorithm we will be using to sign the token
    val signatureAlgorithm: SecureDigestAlgorithm<PrivateKey, PublicKey> = Jwts.SIG.RS256

    val nowMillis = System.currentTimeMillis()
    val now = Date(nowMillis)

    //Let's set the JWT Claims
    val builder: JwtBuilder = Jwts.builder()
        .issuedAt(now)
        .issuer(githubAppId)
        .signWith(privateKey, signatureAlgorithm)

    //if it has been specified, let's add the expiration
    if (ttlMillis > 0) {
        val expMillis = nowMillis + ttlMillis
        val exp = Date(expMillis)
        builder.expiration(exp)
    }

    //Builds the JWT and serializes it to a compact, URL-safe string
    return Jwt(builder.compact(), nowMillis, ttlMillis)
}
