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
            if (server != null)
                server.Stop();

            Console.WriteLine("Hello GodSharp.Socket.TcpServerSample!");

            server = new TcpServer(int.Parse(textBoxPort.Text), textBoxIP.Text)
            {
                OnConnected = (c) =>
                {
                    Console.WriteLine($"{c.RemoteEndPoint} connected.");
                },
                OnReceived = (c) =>
                {
                    Console.WriteLine($"Received from {c.RemoteEndPoint}:");
                    String content = System.Text.Encoding.Default.GetString(c.Buffers);
                    Console.WriteLine(content);

                    int split = content.IndexOf("#"); // # is the spliter between length and data
                    int token_number = int.Parse(content.Substring(0, split));
                    Console.WriteLine("Token number: " + token_number.ToString());

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
                            }
                            catch (ArgumentException)
                            {
                                Console.WriteLine("Invalid Image");
                            }
                        }
                        socketImage.Clear();
                    }
                    else
                    {
                        String imageData = content.Substring(split + 1);
                        socketImage.Append(imageData);
                        Console.WriteLine("Token size: " + imageData.Length.ToString());
                    }
                    byte[] reply = Encoding.ASCII.GetBytes(token_number.ToString() + "\n");
                    c.NetConnection.Send(reply);
                },
                OnDisconnected = (c) =>
                {
                    Console.WriteLine($"{c.RemoteEndPoint} disconnected.");
                },
                OnStarted = (c) =>
                {
                    Console.WriteLine($"{c.LocalEndPoint} started.");
                },
                OnStopped = (c) =>
                {
                    Console.WriteLine($"{c.LocalEndPoint} stopped.");
                },
                OnException = (c) =>
                {
                    Console.WriteLine($"{c.RemoteEndPoint} exception:{c.Exception.StackTrace.ToString()}.");
                },
                OnServerException = (c) =>
                {
                    Console.WriteLine($"{c.LocalEndPoint} exception:{c.Exception.StackTrace.ToString()}.");
                }
            };
            server.Start();
        }

        private void pictureBoxImage_DoubleClick(object sender, EventArgs e)
        {
            Clipboard.SetImage(pictureBoxImage.Image);
        }
    }
}
