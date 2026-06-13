package com.enox.enoxpay.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperDocsScreen(navController: NavController) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Developer Docs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                
                // Header Image / Hero
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "Powerful Payment Gateway",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Automate your mobile banking inputs with Enox Pay's fast SMS parsing and instant webhooks. Build reliable payment systems without native APIs.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://enoxbd.live"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimary,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Web, contentDescription = "Website", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Visit enoxbd.live", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Section: Authentication
                DocSectionTitle("1. Authorization Key")
                DocText("Every request sent from the app contains the Authorization key in the header. To set this up:")
                DocBulletItem("1. Go to Configuration > API Config in this app.")
                DocBulletItem("2. Enter a strong, random string (e.g., 'MySecureSecret_XYZ123') in the Authorization Key field.")
                DocBulletItem("3. In your server-side PHP script, you must check for this exact key. Requests without it should be rejected automatically.")
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Section: MySQL Table Setup
                DocSectionTitle("2. MySQL Database Setup")
                DocText("Create a table in your database to store incoming parsed transactions from the app. Run the following SQL query:")
                
                CodeSnippet(
                    """
                    CREATE TABLE `transactions` (
                        `id` INT AUTO_INCREMENT PRIMARY KEY,
                        `platform` VARCHAR(50) NOT NULL,
                        `transaction_id` VARCHAR(100) UNIQUE NOT NULL,
                        `sender` VARCHAR(50) NOT NULL,
                        `amount` DECIMAL(10,2) NOT NULL,
                        `time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                    """.trimIndent()
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // Section: PHP API Integration
                DocSectionTitle("3. PHP Webhook Script")
                DocText("Create an api.php file on your server (e.g., https://yourdomain.com/api.php). Put this exact URL in the app's Webhook URL setting.")
                DocText("Here is the complete A-Z code you can copy to securely receive data:")
                
                CodeSnippet(
                    """
                    <?php
                    header('Content-Type: application/json');

                    // 1. Verify the Authorization Header
                    ＄headers = apache_request_headers();
                    // Replace 'MySecureSecret_XYZ123' with your set key
                    ＄myAuthKey = 'MySecureSecret_XYZ123';
                    
                    if (!isset(＄headers['Authorization']) || ＄headers['Authorization'] !== ＄myAuthKey) {
                        http_response_code(401);
                        echo json_encode(["status" => "error", "message" => "Unauthorized access."]);
                        exit();
                    }

                    // 2. Database Connection
                    ＄host = 'localhost';
                    ＄db   = 'my_database';
                    ＄user = 'db_user';
                    ＄pass = 'db_pass';

                    // Connect via PDO
                    try {
                        ＄pdo = new PDO("mysql:host=＄host;dbname=＄db", ＄user, ＄pass);
                        ＄pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
                    } catch (PDOException ＄e) {
                        http_response_code(500);
                        echo json_encode(["status" => "error", "message" => "DB Error"]);
                        exit();
                    }

                    // 3. Retrieve JSON payload from the App
                    ＄inputJSON = file_get_contents('php://input');
                    ＄data = json_decode(＄inputJSON, true);

                    if (isset(＄data['transaction_id']) && isset(＄data['amount'])) {
                        // Extract Values
                        ＄trx_id = ＄data['transaction_id'];
                        ＄amount = ＄data['amount'];
                        ＄sender = ＄data['sender'] ?? 'Unknown';
                        ＄platform = ＄data['platform'] ?? 'Generic';

                        // 4. Check if transaction already exists
                        ＄stmt = ＄pdo->prepare("SELECT id FROM transactions WHERE transaction_id = ?");
                        ＄stmt->execute([＄trx_id]);
                        
                        if (＄stmt->rowCount() > 0) {
                            echo json_encode(["status" => "error", "message" => "Duplicate transaction"]);
                            exit();
                        }

                        // 5. Insert new Transaction
                        ＄insert = ＄pdo->prepare("INSERT INTO transactions (platform, transaction_id, sender, amount) VALUES (?, ?, ?, ?)");
                        if (＄insert->execute([＄platform, ＄trx_id, ＄sender, ＄amount])) {
                            // Top up the user's balance based on logic here...
                            
                            echo json_encode(["status" => "success", "message" => "Saved successfully"]);
                        } else {
                            echo json_encode(["status" => "error", "message" => "Failed to save"]);
                        }
                    } else {
                        http_response_code(400);
                        echo json_encode(["status" => "error", "message" => "Invalid payload"]);
                    }
                    ?>
                    """.trimIndent().replace("＄", "$") // Using a different character to avoid escaping issues in json string, replacing it here
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                // Warning note
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Info, contentDescription = "Note", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "The app expects `{ \"status\": \"success\" }` or HTTP code 200 upon successful save. If it receives an error layout or 500 code, it will mark the push as FAILED and retry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun DocSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun DocText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun DocBulletItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp, start = 8.dp)
    )
}

@Composable
fun CodeSnippet(code: String) {
    SelectionContainer {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text = code,
                color = Color(0xFFD4D4D4),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
        }
    }
}
