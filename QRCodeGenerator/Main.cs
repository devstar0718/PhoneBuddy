using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using ZXing.Common;
using ZXing;
using ZXing.QrCode;
using Newtonsoft.Json.Linq;
using GodSharp.Sockets;
using System.IO;

namespace QRCodeGenerator
{
    public partial class Main : Form
    {
        ITcpServer server = null;
        //String socketImage;
        StringBuilder socketImage;
        private delegate void SafeCallDelegate(string text);
        public Main()
        {
            InitializeComponent();
            textBoxIP.Text = "10.10.10.222";
            textBoxPort.Text = "8090";
            socketImage = new StringBuilder();
        }

        private void buttonQRGen_Click(object sender, EventArgs e)
        {
            JObject json = new JObject();
            json["ip"] = textBoxIP.Text;    
            json["port"] = textBoxPort.Text;
            generateQRCode(json.ToString());
        }

        private void generateQRCode(string content)
        {
            if (content.Length == 0)
                return;
            var qr = new ZXing.BarcodeWriter();
            QrCodeEncodingOptions options = new QrCodeEncodingOptions
            {
                DisableECI = true,
                CharacterSet = "UTF-8",
                Width = 250,
                Height = 250,
            };
            qr.Options = options;
            qr.Format = ZXing.BarcodeFormat.QR_CODE;
            var result = new Bitmap(qr.Write(content));
            pictureBoxQR.Image = result;
        }

        private void buttonSockStart_Click(object sender, EventArgs e)
        {
            if(server != null && server.Running)
            {
                server.Stop();
                buttonSockStart.Text = "Socket Start";
                server.Dispose();
                server = null;
                return;
            }

            server = new TcpServer(int.Parse(textBoxPort.Text), textBoxIP.Text)
            {
                OnConnected = (c) =>
                {
                    addLog($"{c.RemoteEndPoint} connected.");
                },
                OnReceived = (c) =>
                {
                    //addLog($"Received from {c.RemoteEndPoint}:");
                    String content = System.Text.Encoding.Default.GetString(c.Buffers);
                    string reply = ParseMessage(content);
                    c.NetConnection.Send(Encoding.ASCII.GetBytes(reply + "\n"));
                },
                OnDisconnected = (c) =>
                {
                    addLog($"{c.RemoteEndPoint} disconnected.");
                },
                OnStarted = (c) =>
                {
                    addLog($"{c.LocalEndPoint} started.");
                },
                OnStopped = (c) =>
                {
                    addLog($"{c.LocalEndPoint} stopped.");
                },
                OnException = (c) =>
                {
                    addLog($"{c.RemoteEndPoint} exception:{c.Exception.StackTrace.ToString()}.");
                },
                OnServerException = (c) =>
                {
                    addLog($"{c.LocalEndPoint} exception:{c.Exception.StackTrace.ToString()}.");
                }
            };
            server.Start();
            buttonSockStart.Text = "Socket Stop";
        }

        private string ParseMessage(string msg)
        {
            var json = JObject.Parse(msg);
            
            string type = json["type"].ToString();
            string content = json["content"].ToString();

            string reply = "";
            if (type == "picture")
            {
                reply = ParsePicture(content);
            }
            else if(type == "order")
            {
                reply = ParseOrder(content);
            }
            return reply;
        }

        private string ParseOrder(string msg)
        {
            var json = JObject.Parse(msg);

            string command = json["command"].ToString();

            if (command == "buy")
            {
                addLog("Received: " + json["part"].ToString() + ", " + json["value"].ToString());
            }
            else if (command == "undo")
            {
                addLog("Received: Undo Last Order");
            }
            return "OK";
        }

        private string ParsePicture(string content)
        {
            //addLog(content);
            int split = content.IndexOf("#"); // # is the spliter between length and data
            int token_number = int.Parse(content.Substring(0, split));
            //addLog("Token number: " + token_number.ToString());
            if (token_number == -1) // end of data
            {
                byte[] data;
                data = Convert.FromBase64String(socketImage.ToString());
                using (var stream = new MemoryStream(data, 0, data.Length))
                {
                    try
                    {
                        Image image = Image.FromStream(stream);
                        //TODO: do something with image
                        pictureBoxImage.Image = image;
                        addLog("Received: Image");
                    }
                    catch (ArgumentException)
                    {
                        addLog("Invalid Image");
                    }
                }
                socketImage.Clear();
            }
            else
            {
                String imageData = content.Substring(split + 1);
                socketImage.Append(imageData);
                //addLog("Token size: " + imageData.Length.ToString());
            }
            return token_number.ToString();
        }

        private void pictureBoxImage_DoubleClick(object sender, EventArgs e)
        {
            Clipboard.SetImage(pictureBoxImage.Image);
        }

        private void addLog(string log)
        {
            if (listBoxLog.InvokeRequired)
            {
                var d = new SafeCallDelegate(addLog);
                listBoxLog.Invoke(d, new object[] { log });
            }
            else
            {
                listBoxLog.Items.Insert(0, log);
            }
        }

        private void buttonSendMsg_Click(object sender, EventArgs e)
        {
            if(server == null)
                return;
            if(textBoxMsg.Text.Length == 0)
            {
                MessageBox.Show("Please input message.");
                textBoxMsg.Focus();
                return;
            }
            var connections = server.Connections;
            foreach(var connect in connections)
            {
                connect.Value.Send("MSG: " + textBoxMsg.Text + "\n");
            }
        }

        private void buttonBeep_Click(object sender, EventArgs e)
        {
            if (server == null)
                return;
            var connections = server.Connections;
            foreach (var connect in connections)
            {
                connect.Value.Send("BEP: \n");
            }
        }
    }
}
