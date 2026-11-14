package com.destiny.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.destiny.agent.ui.Icons
import com.destiny.agent.ui.Panel
import com.destiny.agent.ui.PrimaryButton
import com.destiny.agent.ui.theme.LocalAccents

/**
 * 登录页 —— 参考 Toki 的账户关联做法
 *
 * Toki 的做法是：先讲清"你是谁、这些东西会绑在哪"，再给三种登录方式，
 * 且不强制 —— 可以先进去看。所以这里也保留「先进去看看」。
 *
 * 定位文案统一成新的：它不是聊天机器人，是替你办事的个人智能体。
 */
@Composable
fun LoginScreen(onLogin: (method: String, id: String) -> Unit) {
    val a = LocalAccents.current
    var method by remember { mutableStateOf("google") }
    var ident by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        a.elevated.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .padding(top = 72.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 品牌标记
        Box(
            Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(a.champagne.copy(alpha = 0.14f))
                .border(1.dp, a.champagne.copy(alpha = 0.32f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = Icons.Spark,
                contentDescription = null,
                tint = a.champagne,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Destiny",
            style = MaterialTheme.typography.displayLarge,
            color = a.ink
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "不是聊天机器人。\n是真正替你办事的个人智能体。",
            style = MaterialTheme.typography.bodyLarge,
            color = a.inkSub,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // 三大能力 —— 登录前就说清楚它是什么
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CapCard(Icons.Bolt, "主动执行", "日程、订票这类日常事务它自己办", Modifier.weight(1f))
            CapCard(Icons.Brain, "持久记忆", "持续理解你的需求和上下文", Modifier.weight(1f))
            CapCard(Icons.Monitor, "云端电脑", "复杂网页任务交给云端实例", Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))

        // 登录方式切换
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(a.elevated)
                .padding(8.dp)
        ) {
            listOf(
                "google" to "Google",
                "email" to "邮箱",
                "phone" to "手机号"
            ).forEach { (key, label) ->
                val on = key == method
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (on) a.champagne.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable {
                            method = key
                            ident = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (on) a.champagne else a.inkFaint
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (method == "google") {
            Panel {
                Column {
                    Text(
                        "用 Google 账号继续",
                        style = MaterialTheme.typography.headlineSmall,
                        color = a.ink
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "记忆、日程、已关联的账户会跟着这个账号走。\n" +
                            "不想绑定也可以先进去看 —— 什么都不上传。",
                        style = MaterialTheme.typography.bodySmall,
                        color = a.inkSub
                    )
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("继续", icon = Icons.User) {
                        onLogin("google", "boss@destiny.local")
                    }
                }
            }
        } else {
            Panel {
                Column {
                    Text(
                        if (method == "email") "用邮箱继续" else "用手机号继续",
                        style = MaterialTheme.typography.headlineSmall,
                        color = a.ink
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (method == "email") "邮箱只用于找回账号和同步记忆，不发广告。" else "只用于登录，不发短信骚扰。",
                        style = MaterialTheme.typography.bodySmall,
                        color = a.inkSub
                    )
                    Spacer(Modifier.height(16.dp))
                    IdentField(
                        value = ident,
                        onChange = { ident = it },
                        placeholder = if (method == "email") "you@example.com" else "138 0000 0000",
                        keyboard = if (method == "email") KeyboardType.Email else KeyboardType.Phone
                    )
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(
                        text = "继续",
                        icon = Icons.ArrowUpRight,
                        enabled = ident.trim().length >= 5
                    ) {
                        onLogin(method, ident.trim())
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 先进去看看
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onLogin("guest", "guest") },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "先进去看看",
                style = MaterialTheme.typography.labelLarge,
                color = a.inkSub
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = Icons.Lock,
                contentDescription = null,
                tint = a.inkFaint,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "密码、验证码、卡号永远不进记忆库 —— 引擎层硬规则，不是承诺。",
                style = MaterialTheme.typography.labelSmall,
                color = a.inkFaint,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun CapCard(
    icon: androidx.compose.ui.graphics.painter.Painter,
    title: String,
    note: String,
    modifier: Modifier = Modifier
) {
    val a = LocalAccents.current
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(a.elevated)
            .border(1.dp, a.hairline, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 16.dp)
    ) {
        Icon(icon, null, tint = a.champagne, modifier = Modifier.size(17.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = a.ink
        )
        Spacer(Modifier.height(8.dp))
        Text(
            note,
            style = MaterialTheme.typography.labelSmall,
            color = a.inkFaint
        )
    }
}

@Composable
private fun IdentField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboard: KeyboardType
) {
    val a = LocalAccents.current
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, a.hairline, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = a.inkFaint
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = LocalTextStyle.current.merge(
                TextStyle(color = a.ink, fontSize = 15.sp)
            ),
            cursorBrush = SolidColor(a.champagne),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
