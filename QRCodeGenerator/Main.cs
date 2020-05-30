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
        String socketImage;
        public Main()
        {
            InitializeComponent();
            textBoxIP.Text = "10.10.10.222";
            textBoxPort.Text = "8090";
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
                    //Console.WriteLine(string.Join(" ", c.Buffers.Select(x => x.ToString()).ToArray()));
                    //Console.WriteLine(c.Buffers.ToString());
                    String content = System.Text.Encoding.Default.GetString(c.Buffers);
                    Console.WriteLine(content);
                    socketImage += content;

                    if(content.Contains("*Start*"))
                    {
                    
                    }
                    else if(content.Contains("*End*"))
                    {
                        int start = socketImage.IndexOf("*Start*");
                        start += "*Start*".Length;
                        int end = socketImage.IndexOf("*End*");
                        string realImage = socketImage.Substring(start, end - start);
                        byte[] data;
                        try
                        {
                            data = Convert.FromBase64String(realImage);
                        }
                        catch (System.FormatException)
                        {
                            Console.WriteLine("Invalid Format");
                            socketImage = "";
                            return;
                        }
                        using (var stream = new MemoryStream(data, 0, data.Length))
                        {
                            try
                            {
                                Image image = Image.FromStream(stream);
                                //TODO: do something with image
                                pictureBoxImage.Image = image;
                            }
                            catch(ArgumentException)
                            {
                                Console.WriteLine("Invalid Image");
                            }
                        }
                        socketImage = "";
                    }

                    //c.NetConnection.Send(c.Buffers);
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
    }
}
